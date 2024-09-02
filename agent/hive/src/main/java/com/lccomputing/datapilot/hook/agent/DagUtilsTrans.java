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

public class DagUtilsTrans implements ClassFileTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(DagUtilsTrans.class);
    private static final String CLASS_NAME_DOT = "org.apache.hadoop.hive.ql.exec.tez.DagUtils";
    private static final String CLASS_NAME = CLASS_NAME_DOT.replace('.', '/');
    // @formatter:off
    private static final String CODE =
            "try {\n" +
            "  LOG.info(\"LCC DagUtilsTrans start\");\n" +
            "  com.lccomputing.datapilot.hook.agent.TezHookInstaller.uploadJar($1);\n" +
            "} catch (Exception e) {\n" +
            "  LOG.warn(\"LCC DagUtilsTrans run failed, ignore it and continue: {}\", e.toString());\n" +
            "  LOG.debug(\"LCC DagUtilsTrans run failed, ignore it and continue\", e);\n" +
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

            CtMethod ctMethod = ctClass.getDeclaredMethod("getTempFilesFromConf");
            ctMethod.insertBefore(CODE);
            return ctClass.toBytecode();
        } catch (Exception e) {
            LOG.warn("LCC DagUtilsTrans Failed: {}", e.toString());
            LOG.debug("LCC DagUtilsTrans Failed", e);
            return classfileBuffer;
        }
    }

}
