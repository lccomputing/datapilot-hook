package com.lccomputing.datapilot.hook.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class SparkSubmit240Trans implements ClassFileTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(SparkSubmit240Trans.class);
    private static final String CLASS_NAME = "org/apache/spark/deploy/SparkSubmit";
    private static final String CLASS_NAME_DOT = CLASS_NAME.replace('/', '.');
    // @formatter:off
    private static final String CODE =
            "try {\n" +
            "  com.lccomputing.hbo.solution.agent.SparkDriverAdjuster.adjust(args);\n" +
            "} catch (Exception e) {\n" +
            "  logWarning(new com.lccomputing.hbo.solution.agent.Stringify(\"LCC SparkSubmitHook run failed, ignore it and continue: \" + e));\n" +
            "  logDebug(new com.lccomputing.hbo.solution.agent.Stringify(\"LCC SparkSubmitHook run failed, ignore it and continue\"), e);\n" +
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
            if (!JavassistUtils.has(ctClass, "method", "submit")) {
                return classfileBuffer;
            }

            CtMethod ctMethod = ctClass.getDeclaredMethod("submit");
            ctMethod.insertBefore(CODE);
            return ctClass.toBytecode();
        } catch (Exception e) {
            LOG.warn("LCC HBO SparkSubmitTrans Failed: {}", e.toString());
            LOG.debug("LCC HBO SparkSubmitTrans Failed", e);
            return classfileBuffer;
        }
    }
}
