package com.lccomputing.datapilot.hook.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class DagImplTrans implements ClassFileTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(DagImplTrans.class);
    private static final String CLASS_NAME = "org/apache/tez/dag/app/dag/impl/DAGImpl";
    private static final String CLASS_NAME_DOT = CLASS_NAME.replace('/', '.');
    // @formatter:off
    private static final String CODE =
            "try {\n" +
            "  com.lccomputing.datapilot.hook.agent.DagReporter.report(this, this.getState(), this.finishTime, this.initTime);\n" +
            "} catch (Exception e) {\n" +
            "  LOG.warn(\"LCC Hook DagImplTrans run failed, ignore it and continue: \" + e);\n" +
            "  LOG.debug(\"LCC Hook DagImplTrans run failed, ignore it and continue\", e);\n" +
            "}";
    // @formatter:on

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!CLASS_NAME.equals(className)) {
            return classfileBuffer;
        }

        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(CLASS_NAME_DOT);

            CtMethod ctMethod = ctClass.getDeclaredMethod("isComplete");
            ctMethod.insertAfter(CODE);

            return ctClass.toBytecode();
        } catch (Exception e) {
            LOG.warn("LCC Hook DagImplTrans Failed: {}", e.toString());
            LOG.debug("LCC Hook DagImplTrans Failed", e);
            return classfileBuffer;
        }
    }

}
