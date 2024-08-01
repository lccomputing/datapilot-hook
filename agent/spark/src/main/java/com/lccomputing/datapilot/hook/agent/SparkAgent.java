package com.lccomputing.datapilot.hook.agent;

import java.lang.instrument.Instrumentation;

public class SparkAgent {
    public static void premain(String args, Instrumentation inst) {
        try {
            inst.addTransformer(new SparkSubmitTrans(), true);
            inst.addTransformer(new SparkSubmit240Trans(), true);
        } catch (Exception ex) {
            System.err.println("lcc SparkAgent start failed: " + ex.getMessage() + ", premain args: " + args);
        }
    }
}
