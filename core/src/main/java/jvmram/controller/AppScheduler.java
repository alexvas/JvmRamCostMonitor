package jvmram.controller;

import jvmram.controller.impl.AppSchedulerImpl;

public interface AppScheduler {

    static AppScheduler getInstance() {
        return AppSchedulerImpl.INSTANCE;
    }

    void start();
}
