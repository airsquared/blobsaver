/*
 * Copyright (c) 2021  airsquared
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

import com.sun.jna.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class Utils {

    static final ButtonType redditPM = new ButtonType("PM on Reddit");
    static final ButtonType githubIssue = new ButtonType("Create Issue on Github");

    static final DropShadow errorBorder = new DropShadow(9.5, 0f, 0f, Color.RED);
    static final DropShadow borderGlow = new DropShadow(9.5, 0f, 0f, Color.DARKCYAN);

    private static File tsschecker, blobsaverExecutable;
    private static File licenseFile, librariesUsedFile;

    static ExecutorService threadPool;

    public static void executeInThreadPool(Runnable command) {
        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true); // don't prevent application from exiting
                return t;
            });
        }
        threadPool.execute(command);
    }

    static void checkForUpdates(boolean forceCheck) {
        executeInThreadPool(() -> _checkForUpdates(forceCheck));
    }

    static final record LatestVersion(String version, String changelog) {
        static LatestVersion request() throws IOException {
            JSONObject json = makeRequest("https://api.github.com/repos/airsquared/blobsaver/releases/latest");
            String tempChangelog = json.getString("body");
            return new LatestVersion(json.getString("tag_name"), tempChangelog.substring(tempChangelog.indexOf("Changelog")));
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
            runLater(() -> showReportableError("Unable to check for updates.", e.toString()));
            throw new UncheckedIOException(e);
        }

        if (!Main.appVersion.equals(newVersion.toString()) && (forceCheck || !Prefs.shouldIgnoreVersion(newVersion.toString()))) {
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
                    Prefs.setIgnoreVersion(newVersion.toString());
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

    private static JSONObject makeRequest(String url) throws IOException {
        try (var inputStream = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            return new JSONObject(new JSONTokener(inputStream));
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

    static void newGithubIssue() {
        openURL("https://github.com/airsquared/blobsaver/issues/new/choose");
    }

    static void sendRedditPM() {
        openURL("https://www.reddit.com/message/compose?to=01110101_00101111&subject=Blobsaver%20Bug%20Report");
    }

    static void openURL(String url) {
        Main.JavaFxApplication.getInstance().getHostServices().showDocument(url);
    }

    static String executeProgram(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder logBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                logBuilder.append(line).append("\n");
            }
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return logBuilder.toString();
    }

    static void reportError(Alert alert) {
        if (alert.getResult().equals(githubIssue)) {
            newGithubIssue();
        } else if (alert.getResult().equals(redditPM)) {
            sendRedditPM();
        }
    }

    static void reportError(Alert alert, String toCopy) {
        copyToClipboard(toCopy);
        reportError(alert);
    }

    static void showReportableError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit.", githubIssue, redditPM, ButtonType.CANCEL);
        resizeAlertButtons(alert);
        alert.showAndWait();
        reportError(alert);
    }

    static void showReportableError(String msg, String toCopy) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.", githubIssue, redditPM, ButtonType.CANCEL);
        resizeAlertButtons(alert);
        alert.showAndWait();
        reportError(alert, toCopy);
    }

    static void showUnreportableError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
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
        if (initialDirectory.exists()) {
            dirChooser.setInitialDirectory(initialDirectory);
        } else if (initialDirectory.getParentFile().exists()) {
            dirChooser.setInitialDirectory(initialDirectory.getParentFile());
        } else {
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        return dirChooser.showDialog(window);
    }

    static TextFormatter<String> intOnlyFormatter() {
        return new TextFormatter<>(change -> isNumeric(change.getText()) ? change : null);
    }

    private static boolean isNumeric(String s) {
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
        return StreamSupport.stream(makeRequest(url).getJSONArray("firmwares").spliterator(), false)
                .map(o -> (JSONObject) o)
                .map(o -> new IOSVersion(o.getString("version"), o.getString("url"), o.getBoolean("signed")));
    }

    static Stream<IOSVersion> getSignedFirmwares(String deviceIdentifier) throws IOException {
        return getFirmwareList(deviceIdentifier).filter(IOSVersion::signed);
    }

    public static final record IOSVersion(String versionString, String ipswURL, Boolean signed) {
        public IOSVersion {
            Objects.requireNonNull(ipswURL, "ipsw url cannot be null");
        }
    }

    static Path extractBuildManifest(String ipswUrl) throws IOException {
        Path buildManifest = Files.createTempFile("BuildManifest", ".plist");
        try (ZipFile ipsw = new ZipFile(new HttpChannel(new URL(ipswUrl)), "ipsw", "UTF8", true, true);
             InputStream is = ipsw.getInputStream(ipsw.getEntry("BuildManifest.plist"))) {
            Files.copy(is, buildManifest, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Extracted to " + buildManifest);
        return buildManifest.toRealPath();
    }

    /**
     * Source: https://github.com/jcodec/jcodec/blob/6e1ec651eca92d21b41f9790143a0e6e4d26811e/android/src/main/org/jcodec/common/io/HttpChannel.java
     *
     * @author The JCodec project
     */
    private static final class HttpChannel implements SeekableByteChannel {

        private final URL url;
        private ReadableByteChannel ch;
        private long pos;
        private long length;

        public HttpChannel(URL url) {
            this.url = url;
        }

        @Override
        public long position() {
            return pos;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (newPosition == pos) {
                return this;
            } else if (ch != null) {
                ch.close();
                ch = null;
            }
            pos = newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            ensureOpen();
            return length;
        }

        @Override
        public SeekableByteChannel truncate(long size) {
            throw new UnsupportedOperationException("Truncate on HTTP is not supported.");
        }

        @Override
        public int read(ByteBuffer buffer) throws IOException {
            ensureOpen();
            int read = ch.read(buffer);
            if (read != -1)
                pos += read;
            return read;
        }

        @Override
        public int write(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Write to HTTP is not supported.");
        }

        @Override
        public boolean isOpen() {
            return ch != null && ch.isOpen();
        }

        @Override
        public void close() throws IOException {
            ch.close();
        }

        private void ensureOpen() throws IOException {
            if (ch == null) {
                URLConnection connection = url.openConnection();
                if (pos > 0)
                    connection.addRequestProperty("Range", "bytes=" + pos + "-");
                ch = Channels.newChannel(connection.getInputStream());
                String resp = connection.getHeaderField("Content-Range");
                if (resp != null) {
                    length = Long.parseLong(resp.split("/")[1]);
                } else {
                    resp = connection.getHeaderField("Content-Length");
                    length = Long.parseLong(resp);
                }
            }
        }

    }

    static String exceptionToString(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    static boolean isEmptyOrNull(String s) {
        return s == null || s.isEmpty();
    }

    static <T> T defaultIfNull(T object, T def) {
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
