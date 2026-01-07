module jvmram.core {
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.management;
    requires jdk.attach;
    requires static org.jspecify;
    requires org.slf4j;
    requires jvmram.model;

    exports jvmram.conf;
    exports jvmram.metrics;
    exports jvmram.controller;
    exports jvmram.process;
    exports jvmram.visibility;
}