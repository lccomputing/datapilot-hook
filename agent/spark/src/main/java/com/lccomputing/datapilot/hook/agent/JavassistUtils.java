package com.lccomputing.datapilot.hook.agent;

import javassist.CtClass;
import javassist.NotFoundException;

public class JavassistUtils {
    public static boolean has(CtClass ctClass, String type, String name) {
        try {
            switch (type) {
                case "method":
                    ctClass.getDeclaredMethod(name);
                    break;
                case "field":
                    ctClass.getDeclaredField(name);
                    break;
                default:
                    throw new IllegalArgumentException("unknown type:" + type);
            }
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

}
