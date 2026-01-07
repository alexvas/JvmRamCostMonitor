module jvmram.backend {
    requires org.slf4j;
    requires org.jspecify;

    requires com.google.protobuf;
    requires com.google.common;
    requires io.grpc;
    requires io.grpc.stub;

    requires jvmram.model;
    requires jvmram.core;

    exports jvmram.backend;
}