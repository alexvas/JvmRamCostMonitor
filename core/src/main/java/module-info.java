module jvmram.core {
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.management;
    requires jdk.attach;
    requires static org.jspecify;
    requires org.slf4j;
    exports jvmram.conf;
    exports jvmram.metrics;
    exports jvmram.controller;
    exports jvmram.model.graph;
    exports jvmram.model.metrics;
    exports jvmram.process;
}