package jvmram.controller;

public interface AppScheduler {

    static AppScheduler getInstance() {
        return AppSchedulerImpl.INSTANCE;
    }

    void start();
}
