package com.lccomputing.datapilot.hook.agent;

import com.lccomputing.datapilot.hook.profiling.JvmProfiler;
import com.lccomputing.datapilot.hook.utils.StringUtil;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public class TezAgent {
    @SuppressWarnings("unused")
    public static void premain(String args, Instrumentation inst) {
        try {
            Map<String, String> argMap = StringUtil.splitKv(args, '=', ';');
            System.setProperty("lcc.reporter", argMap.get("reporter"));

            JvmProfiler.run(args);
            inst.addTransformer(new DagImplTrans(), true);
        } catch (Exception ex) {
            System.err.println("LCC TezAgent start failed: " + ex.getMessage() + ", premain args: " + args);
        }
    }
}
