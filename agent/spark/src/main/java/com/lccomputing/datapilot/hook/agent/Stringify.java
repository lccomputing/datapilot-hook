package com.lccomputing.datapilot.hook.agent;

import scala.runtime.AbstractFunction0;

import java.io.Serializable;

public class Stringify extends AbstractFunction0<String> implements Serializable {
    private static final long serialVersionUID = 0L;
    private final String str;

    public Stringify(String str) {
        this.str = str;
    }

    @Override
    public String apply() {
        return str;
    }
}
