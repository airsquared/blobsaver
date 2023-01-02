open module airsquared.blobsaver {
    requires java.prefs;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.crypto.ec; // needed for ssl support in jlink'd image

    requires com.sun.jna;
    requires nsmenufx;
    requires com.google.gson;
    requires org.apache.commons.compress;
    requires info.picocli;
}