package com.lccomputing.datapilot.hook.common;

import java.util.List;

public class RollingString {
    private final List<String> ls;
    private int currentIndex;

    public RollingString(List<String> ls, int currentIndex) {
        this.ls = ls;
        this.currentIndex = currentIndex % ls.size();
    }

    public String get() {
        return ls.get(currentIndex);
    }

    public void next() {
        currentIndex = (currentIndex + 1) % ls.size();
    }

    public int currentIndex() {
        return currentIndex;
    }
}
