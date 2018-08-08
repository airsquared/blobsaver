/*
 * Copyright (c) 2018  airsquared
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

package blobsaver;

import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

import static blobsaver.Main.appPrefs;

// code shared by Controller and Background
class Shared {

    static ButtonType redditPM = new ButtonType("PM on Reddit");
    static ButtonType githubIssue = new ButtonType("Create Issue on Github");

    static String textToIdentifier(String deviceModel) {
        switch (deviceModel) {
            case "iPhone 3G[S]":
                return "iPhone2,1";
            case "iPhone 4 (GSM)":
                return "iPhone3,1";
            case "iPhone 4 (GSM 2012)":
                return "iPhone3,2";
            case "iPhone 4 (CDMA)":
                return "iPhone3,3";
            case "iPhone 4[S]":
                return "iPhone4,1";
            case "iPhone 5 (GSM)":
                return "iPhone5,1";
            case "iPhone 5 (Global)":
                return "iPhone5,2";
            case "iPhone 5c (GSM)":
                return "iPhone5,3";
            case "iPhone 5c (Global)":
                return "iPhone5,4";
            case "iPhone 5s (GSM)":
                return "iPhone6,1";
            case "iPhone 5s (Global)":
                return "iPhone6,2";
            case "iPhone 6+":
                return "iPhone7,1";
            case "iPhone 6":
                return "iPhone7,2";
            case "iPhone 6s":
                return "iPhone8,1";
            case "iPhone 6s+":
                return "iPhone8,2";
            case "iPhone SE":
                return "iPhone8,4";
            case "iPhone 7 (Global)(iPhone9,1)":
                return "iPhone9,1";
            case "iPhone 7+ (Global)(iPhone9,2)":
                return "iPhone9,2";
            case "iPhone 7 (GSM)(iPhone9,3)":
                return "iPhone9,3";
            case "iPhone 7+ (GSM)(iPhone9,4)":
                return "iPhone9,4";
            case "iPhone 8 (iPhone10,1)":
                return "iPhone10,1";
            case "iPhone 8+ (iPhone10,2)":
                return "iPhone10,2";
            case "iPhone X (iPhone10,3)":
                return "iPhone10,3";
            case "iPhone 8 (iPhone10,4)":
                return "iPhone10,4";
            case "iPhone 8+ (iPhone10,5)":
                return "iPhone10,5";
            case "iPhone X (iPhone10,6)":
                return "iPhone10,6";
            case "iPod Touch 3":
                return "iPod3,1";
            case "iPod Touch 4":
                return "iPod4,1";
            case "iPod Touch 5":
                return "iPod5,1";
            case "iPod Touch 6":
                return "iPod7,1";
            case "Apple TV 2G":
                return "AppleTV2,1";
            case "Apple TV 3":
                return "AppleTV3,1";
            case "Apple TV 3 (2013)":
                return "AppleTV3,2";
            case "Apple TV 4 (2015)":
                return "AppleTV5,3";
            case "Apple TV 4K":
                return "AppleTV6,2";
            case "iPad 1":
                return "iPad1,1";
            case "iPad 2 (WiFi)":
                return "iPad2,1";
            case "iPad 2 (GSM)":
                return "iPad2,2";
            case "iPad 2 (CDMA)":
                return "iPad2,3";
            case "iPad 2 (Mid 2012)":
                return "iPad2,4";
            case "iPad Mini (Wifi)":
                return "iPad2,5";
            case "iPad Mini (GSM)":
                return "iPad2,6";
            case "iPad Mini (Global)":
                return "iPad2,7";
            case "iPad 3 (WiFi)":
                return "iPad3,1";
            case "iPad 3 (CDMA)":
                return "iPad3,2";
            case "iPad 3 (GSM)":
                return "iPad3,3";
            case "iPad 4 (WiFi)":
                return "iPad3,4";
            case "iPad 4 (GSM)":
                return "iPad3,5";
            case "iPad 4 (Global)":
                return "iPad3,6";
            case "iPad Air (Wifi)":
                return "iPad4,1";
            case "iPad Air (Cellular)":
                return "iPad4,2";
            case "iPad Air (China)":
                return "iPad4,3";
            case "iPad Mini 2 (WiFi)":
                return "iPad4,4";
            case "iPad Mini 2 (Cellular)":
                return "iPad4,5";
            case "iPad Mini 2 (China)":
                return "iPad4,6";
            case "iPad Mini 3 (WiFi)":
                return "iPad4,7";
            case "iPad Mini 3 (Cellular)":
                return "iPad4,8";
            case "iPad Mini 3 (China)":
                return "iPad4,9";
            case "iPad Mini 4 (Wifi)":
                return "iPad5,1";
            case "iPad Mini 4 (Cellular)":
                return "iPad5,2";
            case "iPad Air 2 (WiFi)":
                return "iPad5,3";
            case "iPad Air 2 (Cellular)":
                return "iPad5,4";
            case "iPad Pro 9.7 (Wifi)":
                return "iPad6,3";
            case "iPad Pro 9.7 (Cellular)":
                return "iPad6,4";
            case "iPad Pro 12.9 (WiFi)":
                return "iPad6,7";
            case "iPad Pro 12.9 (Cellular)":
                return "iPad6,8";
            case "iPad 5 (Wifi)":
                return "iPad6,11";
            case "iPad 5 (Cellular)":
                return "iPad6,12";
            case "iPad Pro 2 12.9 (WiFi)(iPad7,1)":
                return "iPad7,1";
            case "iPad Pro 2 12.9 (Cellular)(iPad7,2)":
                return "iPad7,2";
            case "iPad Pro 10.5 (WiFi)(iPad7,3)":
                return "iPad7,3";
            case "iPad 10.5 (Cellular)(iPad7,4)":
                return "iPad7,4";
            case "iPad 6 (WiFi)(iPad 7,5)":
                return "iPad7,5";
            case "iPad 6 (Cellular)(iPad7,6)":
                return "iPad7,6";
            default: // this will never happen in the background
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not find: \"" + deviceModel + "\"" + "\n\nPlease create a new issue on Github or PM me on Reddit.", new ButtonType("Create Issue on Github"), new ButtonType("PM on Reddit"), ButtonType.CANCEL);
                resizeAlertButtons(alert);
                alert.showAndWait();
                if (alert.getResult().equals(new ButtonType("Create Issue on Github"))) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new/choose"));
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (alert.getResult().equals(new ButtonType("PM on Reddit"))) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
        }
    }

    static void checkForUpdates(boolean forceCheck) {
        Service<Void> service = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        StringBuilder response = new StringBuilder();
                        try {
                            URLConnection urlConnection = new URL("https://api.github.com/repos/airsquared/blobsaver/releases/latest").openConnection();
                            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();
                        } catch (FileNotFoundException ignored) {
                            return null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String newVersion;
                        String changelog;
                        try {
                            newVersion = new JSONObject(response.toString()).getString("tag_name");
                            changelog = new JSONObject(response.toString()).getString("body");
                            changelog = changelog.substring(changelog.indexOf("Changelog"));
                        } catch (JSONException e) {
                            newVersion = Main.appVersion;
                            changelog = "";
                        }
                        if (!newVersion.equals(Main.appVersion) && (forceCheck || !appPrefs.get("Ignore Version", "").equals(newVersion))) {
                            final CountDownLatch latch = new CountDownLatch(1);
                            final String finalNewVersion = newVersion;
                            final String finalChangelog = changelog;
                            Platform.runLater(() -> {
                                try {
                                    ButtonType downloadNow = new ButtonType("Download");
                                    ButtonType ignore = new ButtonType("Ignore this update");
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "You have version " + Main.appVersion + "\n\n" + finalChangelog, downloadNow, ignore, ButtonType.CANCEL);
                                    alert.setHeaderText("New Update Available: " + finalNewVersion);
                                    alert.setTitle("New Update Available for blobsaver");
                                    Button dlButton = (Button) alert.getDialogPane().lookupButton(downloadNow);
                                    dlButton.setDefaultButton(true);
                                    resizeAlertButtons(alert);
                                    alert.showAndWait();
                                    if (alert.getResult().equals(downloadNow) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                        try {
                                            Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/releases/latest"));
                                        } catch (IOException | URISyntaxException ee) {
                                            ee.printStackTrace();
                                        }
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

    static File getTsschecker() throws IOException {
        File tsschecker;
        InputStream input;
        if (PlatformUtil.isWindows()) {
            input = Shared.class.getResourceAsStream("tsschecker_windows.exe");
            tsschecker = File.createTempFile("tsschecker_windows", ".tmp.exe");
        } else if (PlatformUtil.isMac()) {
            input = Shared.class.getResourceAsStream("tsschecker_macos");
            tsschecker = File.createTempFile("tsschecker_macos", ".tmp");
        } else {
            input = Shared.class.getResourceAsStream("tsschecker_linux");
            tsschecker = File.createTempFile("tsschecker_linux", ".tmp");
        }
        OutputStream out = new FileOutputStream(tsschecker);
        int read;
        byte[] bytes = new byte[1024];

        while ((read = input.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        out.close();
        return tsschecker;
    }

    static void newGithubIssue() {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new/choose"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    static void sendRedditPM() {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    static void reportError(Alert alert) {
        if (alert.getResult().equals(githubIssue)) {
            newGithubIssue();
        } else if (alert.getResult().equals(redditPM)) {
            sendRedditPM();
        }
    }

    static void reportError(Alert alert, String toCopy) {
        StringSelection stringSelection = new StringSelection(toCopy);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
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

    static void resizeAlertButtons(Alert alert) {
        alert.getDialogPane().getButtonTypes().stream()
                .map(alert.getDialogPane()::lookupButton)
                .forEach(node -> ButtonBar.setButtonUniformSize(node, false));
    }
}
