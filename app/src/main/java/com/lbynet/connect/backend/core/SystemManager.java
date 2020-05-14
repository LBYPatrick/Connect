package com.lbynet.connect.backend.core;

public class SystemManager {

    private static SystemManager instance = new SystemManager();

    private SystemManager () {

    }

    public void setPowerSavingMode(boolean isTrue) {
        DataPool.isPowerSavingMode = isTrue;
    }

    public void setInvisibleMode(boolean isTrue) {
        DataPool.isInvisibleMode = isTrue;
    }
}
