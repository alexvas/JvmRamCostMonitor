module jvmram.model {
    requires org.slf4j;
    requires java.desktop;

    exports jvmram.model.metrics;
    exports jvmram.model.graph;
    exports jvmram.model.util;
}