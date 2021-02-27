open module airsquared.blobsaver {
    requires java.prefs;
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.crypto.ec; // needed for ssl support in jlink'd image

    requires com.sun.jna;
    requires nsmenufx;
    requires org.json;
}