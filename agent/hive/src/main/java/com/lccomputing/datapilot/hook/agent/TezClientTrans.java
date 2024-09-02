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

public class TezClientTrans implements ClassFileTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(TezClientTrans.class);
    private static final String CLASS_NAME = "org/apache/tez/client/TezClient";
    private static final String CLASS_NAME_DOT = CLASS_NAME.replace('/', '.');
    // @formatter:off
    private static final String CODE =
            "try {\n" +
            "  LOG.info(\"LCC TezClientTrans start\");\n" +
            "  com.lccomputing.datapilot.hook.agent.TezHookInstaller.addAmOpts(amConfig.getTezConfiguration());\n" +
            "} catch (Exception e) {\n" +
            "  LOG.warn(\"LCC TezClientTrans run failed, ignore it and continue: {}\", e.toString());\n" +
            "  LOG.debug(\"LCC TezClientTrans run failed, ignore it and continue\", e);\n" +
            "}";
    // @formatter:on

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!CLASS_NAME.equals(className)) {
            return classfileBuffer;
        }

        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(CLASS_NAME_DOT);

            CtMethod ctMethod = ctClass.getDeclaredMethod("start");
            ctMethod.insertBefore(CODE);
            return ctClass.toBytecode();
        } catch (Exception e) {
            LOG.warn("LCC TezClientTrans Failed: {}", e.toString());
            LOG.debug("LCC TezClientTrans Failed", e);
            return classfileBuffer;
        }
    }

}
