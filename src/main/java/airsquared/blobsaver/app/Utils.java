/*
 * Copyright (c) 2020  airsquared
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

import airsquared.blobsaver.app.natives.Libfragmentzip;
import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Utils {

    static final ButtonType redditPM = new ButtonType("PM on Reddit");
    static final ButtonType githubIssue = new ButtonType("Create Issue on Github");

    static final DropShadow errorBorder = new DropShadow(9.5, 0f, 0f, Color.RED);
    static final DropShadow borderGlow = new DropShadow(9.5, 0f, 0f, Color.DARKCYAN);

    private static File platformDistDir; // only used when running from IDE
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
        executeInThreadPool(() -> {
            String response;
            try {
                response = makeRequest(new URL("https://api.github.com/repos/airsquared/blobsaver/releases/latest"));
            } catch (IOException e) {
                Platform.runLater(() -> showReportableError("Unable to check for updates.", e.toString()));
                throw new UncheckedIOException(e);
            }
            String newVersion;
            String changelog;
            try {
                newVersion = new JSONObject(response).getString("tag_name");
                String tempChangelog = new JSONObject(response).getString("body");
                changelog = tempChangelog.substring(tempChangelog.indexOf("Changelog"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if (!Main.appVersion.equals(newVersion) && (forceCheck || !Prefs.shouldIgnoreVersion(newVersion))) {
                Platform.runLater(() -> {
                    ButtonType downloadNow = new ButtonType("Download");
                    ButtonType ignore = new ButtonType("Ignore this update");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "You have version "
                            + Main.appVersion + "\n\n" + changelog, downloadNow, ignore, ButtonType.CANCEL);
                    alert.setHeaderText("New Update Available: " + newVersion);
                    alert.setTitle("New Update Available for blobsaver");
                    Button dlButton = (Button) alert.getDialogPane().lookupButton(downloadNow);
                    dlButton.setDefaultButton(true);
                    resizeAlertButtons(alert);
                    alert.showAndWait();
                    if (alert.getResult().equals(downloadNow)) {
                        openURL("https://github.com/airsquared/blobsaver/releases");
                    } else if (alert.getResult().equals(ignore)) {
                        Prefs.setIgnoreVersion(newVersion);
                    }
                });
            } else if (forceCheck) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION, "You are on the latest version: " + Main.appVersion);
                    alert.setHeaderText("No updates available");
                    alert.setTitle("No Updates Available");
                    alert.showAndWait();
                });
            }
        });
    }

    private static String makeRequest(URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    static File getPlatformDistDir() {
        if (platformDistDir != null) return platformDistDir;

        platformDistDir = Main.jarDirectory.getParentFile().getParentFile();
        if (platformDistDir.getName().equals("build")) {
            platformDistDir = platformDistDir.getParentFile();
        }
        if (PlatformUtil.isMac()) {
            platformDistDir = new File(platformDistDir, "dist/macos/Contents");
        } else if (PlatformUtil.isWindows()) {
            platformDistDir = new File(platformDistDir, "dist/windows/files");
        } else {
            platformDistDir = new File(platformDistDir, "dist/linux");
        }
        return platformDistDir;
    }

    static File getTsschecker() {
        if (tsschecker != null) return tsschecker;

        if (!Main.runningFromJar) {
            if (PlatformUtil.isWindows()) {
                tsschecker = new File(getPlatformDistDir(), "lib/tsschecker.exe");
            } else if (PlatformUtil.isMac()) {
                tsschecker = new File(getPlatformDistDir(), "MacOS/tsschecker");
            } else {
                tsschecker = new File(getPlatformDistDir(), "tsschecker");
            }
        } else if (PlatformUtil.isMac()) {
            tsschecker = new File(Main.jarDirectory.getParentFile(), "MacOS/tsschecker");
        } else if (PlatformUtil.isWindows()) {
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

        if (!Main.runningFromJar) {
            throw new IllegalStateException();
        }
        return new File(Main.jarDirectory.getParentFile(), "MacOS/JavaAppLauncher");
    }

    static File getLicenseFile() {
        if (licenseFile != null) return licenseFile;

        if (!Main.runningFromJar) {
            licenseFile = new File(getPlatformDistDir().getParentFile(),
                    PlatformUtil.isWindows() ? "dist/windows/LICENSE_windows.txt" : "LICENSE");
        } else if (PlatformUtil.isMac()) {
            licenseFile = new File(Main.jarDirectory.getParentFile(), "Resources/LICENSE");
        } else { // if Linux or Windows
            licenseFile = new File(Main.jarDirectory, "LICENSE");
        }
        licenseFile.setReadOnly();
        return licenseFile;
    }

    static File getLibrariesUsedFile() {
        if (librariesUsedFile != null) return librariesUsedFile;

        if (!Main.runningFromJar) {
            librariesUsedFile = new File(getPlatformDistDir().getParentFile(),
                    PlatformUtil.isWindows() ? "dist/windows/libraries_used_windows.txt" : "libraries_used.txt");
        } else if (PlatformUtil.isMac()) {
            librariesUsedFile = new File(Main.jarDirectory.getParentFile(), "Resources/libraries_used.txt");
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
        alert.showAndWait();
        reportError(alert);
    }

    static void showReportableError(String msg, String toCopy) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.", githubIssue, redditPM, ButtonType.CANCEL);
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
        final Matcher matcher = Pattern.compile("[0-9]*").matcher("");
        return new TextFormatter<>(change -> matcher.reset(change.getText()).matches() ? change : null);
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
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    static List<IOSVersion> getFirmwareList(String deviceIdentifier) throws IOException {
        String response = makeRequest(new URL("https://api.ipsw.me/v4/device/" + deviceIdentifier));
        JSONArray firmwareListJson = new JSONObject(response).getJSONArray("firmwares");
        ArrayList<IOSVersion> iosVersions = new ArrayList<>(firmwareListJson.length());
        firmwareListJson.forEach(o -> {
            JSONObject jo = (JSONObject) o;
            iosVersions.add(new IOSVersion(jo.getString("version"), jo.getString("url"), jo.getBoolean("signed")));
        });

        return iosVersions;
    }

    static List<IOSVersion> getSignedFirmwares(String deviceIdentifier) throws IOException {
        List<IOSVersion> iosVersions = getFirmwareList(deviceIdentifier);
        iosVersions.removeIf(iosVersion -> !iosVersion.signed);
        return iosVersions;
    }

    public static final class IOSVersion {
        public final String versionString, ipswURL;
        public final Boolean signed;

        public IOSVersion(String versionString, String ipswURL, Boolean signed) {
            this.versionString = versionString;
            this.ipswURL = Objects.requireNonNull(ipswURL, "ipsw url cannot be null");
            this.signed = signed;
        }

        @Override
        public String toString() {
            if (versionString == null) {
                return "IOSVersion{'" + ipswURL + "'}";
            }
            return "IOSVersion{'" + versionString + "', '" + ipswURL + "', signed=" + signed + '}';
        }
    }

    static File extractBuildManifest(String url) throws IOException {
        File buildManifest = File.createTempFile("BuildManifest", ".plist");
        System.out.println("Extracting build manifest from " + url);
        int err = Libfragmentzip.downloadFile(url, "BuildManifest.plist", buildManifest.getAbsolutePath());
        if (err != 0) {
            throw new IOException("problem with libfragmentzip download: code=" + err);
        }
        System.out.println("Extracted to " + buildManifest);
        return buildManifest;
    }

    @SuppressWarnings({"rawtypes", "unchecked", "SameParameterValue"})
    static <T> List<T> subList(List list, int endIndex) {
        ArrayList<T> arrayList = new ArrayList<>();
        for (int i = 0; i < endIndex; i++) {
            arrayList.add((T) list.get(i));
        }
        return arrayList;
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
