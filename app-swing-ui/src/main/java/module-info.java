module jvmram.app.swing.ui {
    requires java.datatransfer;
    requires java.desktop;

    requires org.slf4j;

    requires com.google.protobuf;
    requires com.google.common;
    requires io.grpc;

    requires jvmram.model;
    requires java.logging;
    requires org.jspecify;
    requires io.grpc.stub;

    exports jvmram.swing;
}
