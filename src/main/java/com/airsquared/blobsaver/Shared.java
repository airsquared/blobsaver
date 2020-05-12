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

package com.airsquared.blobsaver;

import com.sun.javafx.PlatformUtil;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.airsquared.blobsaver.Main.appPrefs;
import static com.airsquared.blobsaver.Main.appVersion;

// code shared by Controller and Background
@SuppressWarnings("ResultOfMethodCallIgnored")
class Shared {

    static final ButtonType redditPM = new ButtonType("PM on Reddit");
    static final ButtonType githubIssue = new ButtonType("Create Issue on Github");

    static String textToIdentifier(String deviceModel) {
        return Devices.getDeviceModelIdentifiersMap().getOrDefault(deviceModel, "");
    }

    static void checkForUpdates(boolean forceCheck) {
        Service<Void> service = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        String response;
                        try {
                            response = makeRequest(new URL("https://api.github.com/repos/airsquared/blobsaver/releases/latest"));
                        } catch (IOException e) {
                            Platform.runLater(() -> newReportableError("Unable to check for updates.", e.toString()));
                            e.printStackTrace();
                            return null;
                        }
                        String newVersion;
                        String changelog;
                        try {
                            newVersion = new JSONObject(response).getString("tag_name");
                            changelog = new JSONObject(response).getString("body");
                            changelog = changelog.substring(changelog.indexOf("Changelog"));
                        } catch (JSONException e) {
                            newVersion = appVersion;
                            changelog = "";
                        }
                        if (!appVersion.equals(newVersion) &&
                                (forceCheck || !appPrefs.get("Ignore Version", "").equals(newVersion))) {
                            final CountDownLatch latch = new CountDownLatch(1);
                            final String finalNewVersion = newVersion; //so that the lambda works
                            final String finalChangelog = changelog;
                            Platform.runLater(() -> {
                                try {
                                    ButtonType downloadNow = new ButtonType("Download");
                                    ButtonType ignore = new ButtonType("Ignore this update");
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "You have version "
                                            + appVersion + "\n\n" + finalChangelog, downloadNow, ignore, ButtonType.CANCEL);
                                    alert.setHeaderText("New Update Available: " + finalNewVersion);
                                    alert.setTitle("New Update Available for blobsaver");
                                    Button dlButton = (Button) alert.getDialogPane().lookupButton(downloadNow);
                                    dlButton.setDefaultButton(true);
                                    resizeAlertButtons(alert);
                                    alert.showAndWait();
                                    if (alert.getResult().equals(downloadNow)) {
                                        openURL("https://github.com/airsquared/blobsaver/releases");
                                    } else if (alert.getResult().equals(ignore)) {
                                        appPrefs.put("Ignore Version", finalNewVersion);
                                    }
                                } finally {
                                    latch.countDown();
                                }
                            });
                            latch.await();
                        }
                        return null;
                    }
                };
            }
        };
        service.start();
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

    static File getTsschecker() {
        File tsschecker;
        if (Main.runningFromIDE) {
            // temporarily set tsschecker to the dist directory
            tsschecker = new File(Main.jarDirectory.getParentFile().getParentFile().getParentFile(), "dist/");
            if (PlatformUtil.isMac()) {
                tsschecker = new File(tsschecker, "macos/tsschecker");
            } else if (PlatformUtil.isWindows()) {
                tsschecker = new File(tsschecker, "windows/tsschecker.exe");
            } else {
                tsschecker = new File(tsschecker, "linux/tsschecker");
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

    private static void copyStreamToFile(InputStream inputStream, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        int read;
        byte[] bytes = new byte[1024];

        while ((read = inputStream.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        out.close();
        inputStream.close();
    }

    static void newGithubIssue() {
        openURL("https://github.com/airsquared/blobsaver/issues/new/choose");
    }

    static void sendRedditPM() {
        openURL("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report");
    }

    static void openURL(String url) {
        if (PlatformUtil.isMac()) {
            try {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Main.JavaFxApplication.getInstance().getHostServices().showDocument(url);
        }
    }

    static String executeProgram(String... command) throws IOException {
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

    static void newReportableError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit.", githubIssue, redditPM, ButtonType.CANCEL);
        alert.showAndWait();
        reportError(alert);
    }

    static void newReportableError(String msg, String toCopy) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.", githubIssue, redditPM, ButtonType.CANCEL);
        alert.showAndWait();
        reportError(alert, toCopy);
    }

    static void newUnreportableError(String msg) {
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

    static void runSafe(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    static List<Map<String, Object>> getFirmwareList(String deviceIdentifier) throws IOException {
        String response = makeRequest(new URL("https://api.ipsw.me/v4/device/" + deviceIdentifier));
        JSONArray firmwareListJson = new JSONObject(response).getJSONArray("firmwares");
        @SuppressWarnings("unchecked") List<Map<String, Object>> firmwareList = (List) firmwareListJson.toList();
        return firmwareList;
    }

    static List<Map<String, Object>> getAllSignedFirmwares(String deviceIdentifier) throws IOException {
        return getFirmwareList(deviceIdentifier).stream().filter(map -> Boolean.TRUE.equals(map.get("signed"))).collect(Collectors.toList());
    }

//    static List<String> getAllSignedVersions(String deviceIdentifier) throws IOException {
//        return getAllSignedFirmwares(deviceIdentifier).map(map -> map.get("version").toString()).collect(Collectors.toList());
//    }

    static File extractBuildManifest(String url) throws IOException {
        File buildManifest = File.createTempFile("BuildManifest", ".plist");
        System.out.println("Extracting build manifest from " + url);
        Pointer fragmentzip = Libfragmentzip.open(url);
        try {
            int err = Libfragmentzip.downloadFile(fragmentzip, "BuildManifest.plist", buildManifest.getAbsolutePath());
            if (err != 0) {
                throw new IOException("problem with libfragmentzip download: code=" + err);
            }
            System.out.println("Extracted to " + buildManifest);
            return buildManifest;
        } finally {
            Libfragmentzip.close(fragmentzip);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class Libfragmentzip {

        public static Pointer open(String url) {
            return fragmentzip_open(url);
        }

        public static int downloadFile(Pointer fragmentzip_t, String remotePath, String savePath) {
            return fragmentzip_download_file(fragmentzip_t, remotePath, savePath, null);
        }

        public static void close(Pointer fragmentzip_t) {
            fragmentzip_close(fragmentzip_t);
        }

        /**
         * Native declaration:
         * <code>fragmentzip_t *fragmentzip_open(const char *url);</code>
         */
        private static native Pointer fragmentzip_open(String url);

        /**
         * Native declaration:
         * <code>int fragmentzip_download_file(fragmentzip_t *info, const char *remotepath, const char *savepath, fragmentzip_process_callback_t callback);</code>
         */
        private static native int fragmentzip_download_file(Pointer fragmentzip_t, String remotePath, String savePath, Pointer fragmentzip_process_callback_t);

        private static native void fragmentzip_close(Pointer fragmentzip_t);

        static {
            Native.register(Libfragmentzip.class, "fragmentzip");
        }
    }

    static String exceptionToString(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    // temporary until ProGuard is implemented
    static boolean containsIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (regionMatches(str, i, searchStr, len)) {
                return true;
            }
        }
        return false;
    }

    // temporary until ProGuard is implemented
    private static boolean regionMatches(final CharSequence cs, final int thisStart,
                                         final CharSequence substring, final int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(true, thisStart, (String) substring, 0, length);
        }
        int index1 = thisStart;
        int index2 = 0;
        int tmpLen = length;

        // Extract these first so we detect NPEs the same as the java.lang.String version
        final int srcLen = cs.length() - thisStart;
        final int otherLen = substring.length();

        // Check for invalid parameters
        if (thisStart < 0 || length < 0) {
            return false;
        }

        // Check that the regions are long enough
        if (srcLen < length || otherLen < length) {
            return false;
        }

        while (tmpLen-- > 0) {
            final char c1 = cs.charAt(index1++);
            final char c2 = substring.charAt(index2++);

            if (c1 == c2) {
                continue;
            }

            // The same check as in String.regionMatches():
            if (Character.toUpperCase(c1) != Character.toUpperCase(c2)
                    && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false;
            }
        }

        return true;
    }
}
