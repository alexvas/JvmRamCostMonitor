module jvmram.app.swing.ui {
    requires java.datatransfer;
    requires java.desktop;
    requires org.slf4j;
    requires com.google.protobuf;
    requires com.google.common;
    requires jvmram.model;

    exports jvmram.swing;
}
