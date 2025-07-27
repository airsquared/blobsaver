/*
 * Copyright (c) 2023  airsquared
 *
 * This file is part of blobsaver.
 *
 * blobsaver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * blobsaver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with blobsaver.  If not, see <https://www.gnu.org/licenses/>.
 */

package airsquared.blobsaver.app;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.sun.jna.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class Utils {

    static final ButtonType githubIssue = new ButtonType("Create Issue on GitHub");

    static final DropShadow errorBorder = new DropShadow(9.5, 0f, 0f, Color.RED);
    static final DropShadow borderGlow = new DropShadow(9.5, 0f, 0f, Color.DARKCYAN);

    private static File tsschecker, blobsaverExecutable;
    private static File licenseFile, librariesUsedFile;

    private final static ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        var t = new Thread(r);
        t.setDaemon(true); // don't prevent application from exiting
        return t;
    });

    public static void executeInThreadPool(Runnable command) {
        threadPool.execute(command);
    }

    static void checkForUpdates(boolean forceCheck) {
        executeInThreadPool(() -> _checkForUpdates(forceCheck));
    }

    record LatestVersion(String version, String changelog) {
        static LatestVersion request() throws IOException {
            JsonElement json = Network.makeJsonRequest("https://api.github.com/repos/airsquared/blobsaver/releases/latest");
            String tempChangelog = json.getAsJsonObject().get("body").getAsString();
            return new LatestVersion(json.getAsJsonObject().get("tag_name").getAsString(), tempChangelog.substring(tempChangelog.indexOf("Changelog")));
        }

        @Override
        public String toString() {
            return version;
        }
    }

    private static void _checkForUpdates(boolean forceCheck) {
        LatestVersion newVersion;
        try {
            newVersion = LatestVersion.request();
        } catch (IOException e) {
            runLater(() -> showReportableError("Unable to check for updates.", exceptionToString(e)));
            throw new UncheckedIOException(e);
        }

        if (!Main.appVersion.equals(newVersion.version()) && (forceCheck || !Prefs.shouldIgnoreVersion(newVersion.version()))) {
            runLater(() -> {
                ButtonType downloadNow = new ButtonType("Download");
                ButtonType ignore = new ButtonType("Ignore this update");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You have version "
                        + Main.appVersion + "\n\n" + newVersion.changelog(), downloadNow, ignore, ButtonType.CANCEL);
                alert.setHeaderText("New Update Available: " + newVersion);
                alert.setTitle("New Update Available for blobsaver");
                Button dlButton = (Button) alert.getDialogPane().lookupButton(downloadNow);
                dlButton.setDefaultButton(true);
                resizeAlertButtons(alert);
                alert.showAndWait();
                if (alert.getResult().equals(downloadNow)) {
                    openURL("https://github.com/airsquared/blobsaver/releases");
                } else if (alert.getResult().equals(ignore)) {
                    Prefs.setIgnoreVersion(newVersion.version());
                }
            });
        } else if (forceCheck) {
            runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You are on the latest version: " + Main.appVersion);
                alert.setHeaderText("No updates available");
                alert.setTitle("No Updates Available");
                alert.showAndWait();
            });
        }
    }

    static File getTsschecker() {
        if (tsschecker != null) return tsschecker;

        if (Platform.isMac()) {
            tsschecker = new File(Main.jarDirectory, "MacOS/tsschecker");
        } else if (Platform.isWindows()) {
            tsschecker = new File(Main.jarDirectory, "lib/tsschecker.exe");
        } else {
            tsschecker = new File(Main.jarDirectory, "tsschecker");
        }
        System.out.println("tsschecker = " + tsschecker.getAbsolutePath());
        tsschecker.setReadable(true, false);
        tsschecker.setExecutable(true, false);
        return tsschecker;
    }

    static File getBlobsaverExecutable() {
        if (blobsaverExecutable != null) return blobsaverExecutable;

        if (Platform.isMac()) {
            blobsaverExecutable = new File(Main.jarDirectory, "MacOS/blobsaver");
        } else if (Platform.isWindows()) {
            blobsaverExecutable = new File(Main.jarDirectory, "blobsaver.exe");
        } else {
            blobsaverExecutable = new File(Main.jarDirectory.getParentFile(), "bin/blobsaver");
        }

        return blobsaverExecutable;
    }

    static File getLicenseFile() {
        if (licenseFile != null) return licenseFile;

        if (Platform.isMac()) {
            licenseFile = new File(Main.jarDirectory, "Resources/LICENSE");
        } else { // if Linux or Windows
            licenseFile = new File(Main.jarDirectory, "LICENSE");
        }
        licenseFile.setReadOnly();
        return licenseFile;
    }

    static File getLibrariesUsedFile() {
        if (librariesUsedFile != null) return librariesUsedFile;

        if (Platform.isMac()) {
            librariesUsedFile = new File(Main.jarDirectory, "Resources/libraries_used.txt");
        } else { // if Linux or Windows
            librariesUsedFile = new File(Main.jarDirectory, "libraries_used.txt");
        }
        librariesUsedFile.setReadOnly();
        return librariesUsedFile;
    }

    static void clearAppData() {
        try {
            if (Background.isBackgroundEnabled()) {
                Background.stopBackground();
            }
            Background.deleteBackgroundFile();
        } catch (Exception ignored) {
        }
        Prefs.resetPrefs();
    }

    static void newGithubIssue() {
        openURL("https://github.com/airsquared/blobsaver/issues/new/choose");
    }

    static void sendRedditPM() {
        openURL("https://www.reddit.com/message/compose?to=01110101_00101111&subject=Blobsaver");
    }

    static void openURL(String url) {
        Main.JavaFxApplication.getInstance().getHostServices().showDocument(url);
    }

    static String executeProgram(List<String> command) throws IOException {
        return executeProgram(command, true);
    }

    static String executeProgram(List<String> command, boolean print) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String log;
        try (var reader = process.inputReader()) {
            if (print) {
                log = reader.lines().peek(System.out::println).collect(Collectors.joining("\n"));
            } else {
                log = reader.lines().collect(Collectors.joining("\n"));
            }
            process.waitFor();
            return log;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void reportError(Alert alert) {
        if (alert.getResult().equals(githubIssue)) {
            newGithubIssue();
        }
    }

    static void reportError(Alert alert, String toCopy) {
        copyToClipboard(toCopy);
        reportError(alert);
    }

    static void showReportableError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nIf you need more help, you can create a new issue on GitHub.", githubIssue, ButtonType.CANCEL);
        resizeAlertButtons(alert);
        alert.showAndWait();
        reportError(alert);
    }

    static void showReportableError(String msg, String toCopy) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nThe log has been copied to your clipboard. If you need more help, you can create a new issue on GitHub.", githubIssue, ButtonType.CANCEL);
        resizeAlertButtons(alert);
        alert.showAndWait();
        reportError(alert, toCopy);
    }

    static ButtonType showUnreportableError(String msg, ButtonType... buttons) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, buttons);
        resizeAlertButtons(alert);
        alert.showAndWait();
        return alert.getResult();
    }

    static ButtonType showUnreportableError(String msg, String toShow, ButtonType... buttons) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, buttons);
        resizeAlertButtons(alert);
        var text = new TextArea(toShow);
        text.setEditable(false);
        alert.getDialogPane().setExpandableContent(text);
        alert.showAndWait();
        return alert.getResult();
    }

    static void showInfoAlert(String msg, ButtonType... buttons) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, buttons);
        alert.showAndWait();
    }

    static ButtonType showConfirmAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg);
        alert.showAndWait();
        return alert.getResult();
    }

    static void copyToClipboard(String s) {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(s);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    static void resizeAlertButtons(Alert alert) {
        forEachButton(alert, button -> ButtonBar.setButtonUniformSize(button, false));
    }

    static void forEachButton(Alert alert, Consumer<? super Node> action) {
        runSafe(() -> alert.getDialogPane().getButtonTypes().stream().map(alert.getDialogPane()::lookupButton).forEach(action));
    }

    static File showFilePickerDialog(Window window, File initialDirectory) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose a folder to save Blobs in");
        for (int i = 0; i < 20; i++) { // limit loop iterations to 20
            if (initialDirectory == null || initialDirectory.isDirectory()) {
                dirChooser.setInitialDirectory(initialDirectory);
                break;
            } else {
                initialDirectory = initialDirectory.getParentFile();
            }
        }
        return dirChooser.showDialog(window);
    }

    static TextFormatter<String> intOnlyFormatter() {
        return new TextFormatter<>(change -> isNumeric(change.getText()) ? change : null);
    }

    static boolean isNumeric(String s) {
        if (isEmptyOrNull(s)) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean isFieldEmpty(CheckBox checkBox, TextField textField) {
        return isFieldEmpty(checkBox.isSelected(), textField.getText(), textField);
    }

    static boolean isFieldEmpty(boolean isFieldRequired, TextField textField) {
        return isFieldEmpty(isFieldRequired, textField.getText(), textField);
    }

    static boolean isFieldEmpty(boolean isFieldRequired, String fieldValue, Node node) {
        if (isFieldRequired && Utils.isEmptyOrNull(fieldValue)) {
            node.setEffect(errorBorder);
            return true;
        }
        return false;
    }

    static void setSelectedFire(CheckBox box, boolean selected) {
        if (box.isSelected() != selected) {
            box.fire();
        }
    }

    static void runSafe(Runnable runnable) {
        if (isFxApplicationThread()) {
            runnable.run();
        } else {
            runLater(runnable);
        }
    }

    static Stream<IOSVersion> getFirmwareList(String deviceIdentifier) throws IOException {
        String url = "https://api.ipsw.me/v4/device/" + deviceIdentifier;
        try {
            return createVersionStream(Network.makeJsonRequest(url).getAsJsonObject().getAsJsonArray("firmwares"));
        } catch (IOException e) {
            try {
                return getBetaHubList(deviceIdentifier, false);
            } catch (Exception ex) {
                e.addSuppressed(ex);
                throw e;
            }
        }
    }

    static Stream<IOSVersion> getBetaList(String deviceIdentifier) throws IOException {
        return getBetaHubList(deviceIdentifier, true);
    }

    static Stream<IOSVersion> getBetaHubList(String deviceIdentifier, boolean betas) throws IOException {
        String url = "https://www.betahub.cn/api/apple/firmwares/" + deviceIdentifier + "?type=" + (betas ? 2 : 1);

        JsonElement response = Network.makeJsonRequest(url);
        try {
            var firmwares = response.getAsJsonObject().getAsJsonArray("firmwares");
            return StreamSupport.stream(firmwares.spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .map(o -> new IOSVersion(o.get("version").getAsString(), o.get("build_id").getAsString(), o.get("url").getAsString(), o.get("signing").getAsInt() == 1));
        } catch (NullPointerException e) {
            throw new NoSuchElementException("Unable to extract iOS Versions from JSON:\n" + response, e);
        }
    }

    private static Stream<IOSVersion> createVersionStream(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(o -> new IOSVersion(o.get("version").getAsString(), o.get("buildid").getAsString(), o.get("url").getAsString(), o.get("signed").getAsBoolean()));
    }

    static Stream<IOSVersion> getSignedFirmwares(String deviceIdentifier) throws IOException {
        return getFirmwareList(deviceIdentifier).filter(IOSVersion::signed);
    }

    static Stream<IOSVersion> getSignedBetas(String deviceIdentifier) throws IOException {
        return getBetaList(deviceIdentifier).filter(IOSVersion::signed);
    }

    record IOSVersion(String versionString, String buildid, String ipswURL, Boolean signed) {
        public IOSVersion {
            Objects.requireNonNull(ipswURL, "ipsw url cannot be null");
        }
    }

    // assumes that ipswUrl has been checked with `new URI(ipswUrl)`
    static Path extractBuildManifest(String ipswUrl) throws IOException {
        Path buildManifest = Files.createTempFile("BuildManifest", ".plist");
        buildManifest.toFile().deleteOnExit();
        if (ipswUrl.matches("https?://.*apple.*\\.ipsw")) {
            var fileName = Path.of(URI.create(ipswUrl).getPath()).getFileName().toString();
            var manifestURL = ipswUrl.replace(fileName, "BuildManifest.plist");
            try {
                return Network.downloadFile(manifestURL, buildManifest).body().toRealPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (ipswUrl.startsWith("file:") && ipswUrl.endsWith(".plist")) {
            Files.copy(Path.of(URI.create(ipswUrl)), buildManifest, StandardCopyOption.REPLACE_EXISTING);
            return buildManifest.toRealPath();
        } else if (ipswUrl.endsWith(".plist")) {
            try {
                buildManifest = Network.downloadFile(ipswUrl, buildManifest).body().toRealPath();
                System.out.println("Directly downloaded to " + buildManifest);
                return buildManifest;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        extractManifestFromZip(ipswUrl, buildManifest);
        System.out.println("Extracted to " + buildManifest);
        return buildManifest.toRealPath();
    }

    private static void extractManifestFromZip(String ipswUrl, Path extractTo) throws IOException {
        SeekableByteChannel channel = ipswUrl.startsWith("file:")
                ? Files.newByteChannel(Path.of(URI.create(ipswUrl)), StandardOpenOption.READ)
                : new Network.HttpChannel(URI.create(ipswUrl).toURL());
        try (channel; var ipsw = new ZipFile(channel, "ipsw", "UTF8", true, true);
             var stream = ipsw.getInputStream(ipsw.getEntry("BuildManifest.plist"))) {
            Files.copy(stream, extractTo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Convert the byte array to a hex string with respect to the given endianness
     * Source: https://stackoverflow.com/a/58118078/5938387
     * <p>
     * Maybe replace with https://github.com/patrickfav/bytes-java ?
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public static String bytesToHex(byte[] byteArray, ByteOrder byteOrder) {
        final char[] lookup = new char[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66};

        // our output size will be exactly 2x byte-array length
        final char[] buffer = new char[byteArray.length * 2];

        int index;
        for (int i = 0; i < byteArray.length; i++) {
            // for little endian we count from last to first
            index = (byteOrder == ByteOrder.BIG_ENDIAN) ? i : byteArray.length - i - 1;

            // extract the upper 4 bit and look up char (0-A)
            buffer[i << 1] = lookup[(byteArray[index] >> 4) & 0xF];
            // extract the lower 4 bit and look up char (0-A)
            buffer[(i << 1) + 1] = lookup[(byteArray[index] & 0xF)];
        }
        return new String(buffer);
    }

    static String exceptionToString(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    static boolean isEmptyOrNull(String s) {
        return s == null || s.isEmpty();
    }

    static <T> T defIfNull(T object, T def) {
        return (object == null) ? def : object;
    }

    static boolean containsIgnoreCase(final String str, final String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

}
