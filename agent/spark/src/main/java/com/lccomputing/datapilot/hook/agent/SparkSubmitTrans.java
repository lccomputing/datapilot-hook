/*
 * Lccomputing Sky DataPilot Hook
 * Copyright 2021 Lccomputing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lccomputing.datapilot.hook.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

public class SparkSubmitTrans implements ClassFileTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(SparkSubmitTrans.class);
    private static final Set<String> CLASS_NAME;
    static {
        CLASS_NAME = new HashSet<>(2);
        CLASS_NAME.add("org/apache/spark/deploy/SparkSubmit$");
        CLASS_NAME.add("org/apache/spark/deploy/SparkSubmit");
    }

    // @formatter:off
    private static final String CODE_GE_230 =
            "try {\n" +
            "  com.lccomputing.datapilot.hook.agent.SparkDriverAdjuster.adjust($1);\n" +
            "} catch (Exception e) {\n" +
            "  logWarning(new com.lccomputing.datapilot.hook.agent.Stringify(\"LCC SparkSubmitTrans run failed, ignore it and continue: \" + e));\n" +
            "  logDebug(new com.lccomputing.datapilot.hook.agent.Stringify(\"LCC SparkSubmitTrans run failed, ignore it and continue\"), e);\n" +
            "}";

    private static final String CODE_LT_230 =
            "try {\n" +
            "  com.lccomputing.datapilot.hook.agent.SparkDriverAdjuster.adjust($1);\n" +
            "} catch (Exception e) {\n" +
            "  printWarning(\"LCC SparkSubmitTrans run failed, ignore it and continue: \" + e);\n" +
            "}";
    // @formatter:on

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!CLASS_NAME.contains(className)) {
            return classfileBuffer;
        }

        try {
            String classNameDot = className.replace('/', '.');
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(classNameDot);
            if (!JavassistUtils.has(ctClass, "method", "submit")) {
                return classfileBuffer;
            }

            String codeName;
            String code;
            if (JavassistUtils.has(ctClass, "method", "printWarning")) {
                code = CODE_LT_230;
                codeName = "CODE_LT_230";
            } else if (JavassistUtils.has(ctClass, "method", "logWarning")) {
                code = CODE_GE_230;
                codeName = "CODE_GE_230";
            } else {
                throw new RuntimeException("LCC DataPilot SparkSubmitTrans Failed: not found the log method");
            }
            LOG.info("LCC DataPilot SparkSubmitTrans transform {}.submit of code: {}", classNameDot, codeName);

            CtMethod ctMethod = ctClass.getDeclaredMethod("submit");
            ctMethod.insertBefore(code);
            return ctClass.toBytecode();
        } catch (Exception e) {
            LOG.warn("LCC DataPilot SparkSubmitTrans Failed: {}", e.toString());
            LOG.debug("LCC DataPilot SparkSubmitTrans Failed", e);
            return classfileBuffer;
        }
    }
}
