/*
 * Copyright (c) 2019  airsquared
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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static com.airsquared.blobsaver.Main.appPrefs;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

// code shared by Controller and Background
@SuppressWarnings("ResultOfMethodCallIgnored")
class Shared {

    static ButtonType redditPM = new ButtonType("PM on Reddit");
    static ButtonType githubIssue = new ButtonType("Create Issue on Github");
    static HashMap<String, String> deviceModels = initializeDeviceModels();

    private static HashMap<String, String> initializeDeviceModels() {
        try {
            Properties properties = new Properties();
            properties.load(Shared.class.getResourceAsStream("devicemodels.properties"));
            @SuppressWarnings("unchecked") Map<String, String> prop = ((Map) properties); // so I can avoid "unchecked call" warning
            HashMap<String, String> hashMap = new HashMap<>(prop);
            prop.forEach((key, value) -> hashMap.put(value, key));
            return hashMap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static String textToIdentifier(String deviceModel) {
        String toReturn = deviceModels.getOrDefault(deviceModel, "");
        if (toReturn.equals("")) { // this will never happen in background
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
        } else {
            return toReturn;
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
        if (PlatformUtil.isWindows()) {
            return getTsscheckerWindows();
        }
        File executablesFolder = getExecutablesFolder();
        File tsschecker = new File(executablesFolder, "tsschecker");
        if (tsschecker.exists() && appPrefs.getBoolean("tsschecker last update v2.2.3", false)) {
            return tsschecker;
        } else {
            InputStream input;
            if (PlatformUtil.isMac()) {
                input = Shared.class.getResourceAsStream("tsschecker_macos");
            } else {
                input = Shared.class.getResourceAsStream("tsschecker_linux");
            }
            tsschecker.createNewFile();
            OutputStream out = new FileOutputStream(tsschecker);
            int read;
            byte[] bytes = new byte[1024];

            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.close();
            tsschecker.setReadable(true, false);
            tsschecker.setExecutable(true, false);
            appPrefs.putBoolean("tsschecker last update v2.2.3", true);
            return tsschecker;
        }
    }

    private static File getTsscheckerWindows() throws IOException {
        File tsscheckerDir = new File(getExecutablesFolder(), "tsschecker_windows");
        File tsschecker = new File(tsscheckerDir, "tsschecker.exe");
        if (tsschecker.exists() && appPrefs.getBoolean("tsschecker last update v2.2.3", false)) {
            return tsschecker;
        }
        tsscheckerDir.mkdir();
        String jarPath = "/com/airsquared/blobsaver/" + "tsschecker_windows";
        copyDirFromJar(jarPath, tsscheckerDir.toPath());
        appPrefs.putBoolean("tsschecker last update v2.2.3", true);
        tsschecker.setReadable(true);
        tsschecker.setExecutable(true, false);
        return tsschecker;
    }

    static File getidevicepair() throws IOException {
        File idevicepair = new File(getlibimobiledeviceFolder(), "idevicepair");
        if (idevicepair.exists()) {
            return idevicepair;
        } else if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            String executablePath = new BufferedReader(new InputStreamReader(new ProcessBuilder("which", "idevicepair").redirectErrorStream(true).start().getInputStream())).readLine();
            if (executablePath == null) {
                // Calling function must catch FileNotFoundException and show error message to user saying that "idevicepair" is not in the $PATH or is not installed
                throw new FileNotFoundException("idevicepair is not in $PATH");
            }
            return new File(executablePath);
        }
        if (PlatformUtil.isMac()) {
            return new File(getlibimobiledeviceFolder(), "idevicepair");
        } else {
            return new File(getlibimobiledeviceFolder(), "idevicepair.exe");
        }
    }

    static File getideviceinfo() throws IOException {
        File ideviceinfo = new File(getlibimobiledeviceFolder(), "ideviceinfo");
        if (ideviceinfo.exists()) {
            return ideviceinfo;
        } else if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            String executablePath = new BufferedReader(new InputStreamReader(new ProcessBuilder("which", "ideviceinfo").redirectErrorStream(true).start().getInputStream())).readLine();
            if (executablePath == null) {
                // Calling function must catch FileNotFoundException and show error message to user saying that "ideviceinfo" is not in the $PATH or is not installed
                throw new FileNotFoundException("ideviceinfo is not in $PATH");
            }
            return new File(executablePath);
        }
        // need to create ideviceinfo for macOS or Windows
        if (PlatformUtil.isMac()) {
            return new File(getlibimobiledeviceFolder(), "ideviceinfo");
        } else {
            return new File(getlibimobiledeviceFolder(), "ideviceinfo.exe");
        }
    }

    @SuppressWarnings("Duplicates")
    private static File getlibimobiledeviceFolder() throws IOException {
        File libimobiledeviceFolder;
        if (PlatformUtil.isMac()) {
            libimobiledeviceFolder = new File(getExecutablesFolder(), "libimobiledevice_mac/");
        } else {
            libimobiledeviceFolder = new File(getExecutablesFolder(), "libimobiledevice_windows/");
        }
        if (libimobiledeviceFolder.exists()) {
            return libimobiledeviceFolder;
        } else {
            libimobiledeviceFolder.mkdir();
            if (Shared.class.getResource("Shared.class").toString().startsWith("jar:")) { // if being run from jar
                final String jarPath;
                if (PlatformUtil.isMac()) {
                    jarPath = "/com/airsquared/blobsaver/" + "libimobiledevice_mac";
                } else {
                    jarPath = "/com/airsquared/blobsaver/" + "libimobiledevice_windows";
                }
                copyDirFromJar(jarPath, libimobiledeviceFolder.toPath());
                try (Stream<Path> paths = Files.walk(new File(System.getProperty("user.home"), ".blobsaver_bin").toPath())) {
                    paths.forEach(path -> {
                        System.out.println("in for each loop");
                        File file = path.toFile();
                        String fileName = file.getName();
                        if (!file.isDirectory() && (fileName.contains("ideviceinfo") || fileName.contains("idevicepair") || fileName.contains("iproxy"))) {
                            System.out.println("setting " + fileName + " to readable and executable");
                            file.setReadable(true, false);
                            file.setExecutable(true, false);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                System.out.println("returning from libimobiledevice");
                return libimobiledeviceFolder;
            } else { // if being run directly from IDEA
                System.out.println("run from idea");
                final Path targetPath = libimobiledeviceFolder.toPath();
                final Path sourcePath;
                try {
                    if (PlatformUtil.isMac()) {
                        sourcePath = Paths.get(Shared.class.getResource("libimobiledevice_mac").toURI());
                    } else {
                        sourcePath = Paths.get(Shared.class.getResource("libimobiledevice_windows").toURI());
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return null;
                }
                Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(final Path dir,
                                                             final BasicFileAttributes attrs) throws IOException {
                        Files.createDirectories(targetPath.resolve(sourcePath
                                .relativize(dir)));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(final Path file,
                                                     final BasicFileAttributes attrs) throws IOException {
                        Files.copy(file,
                                targetPath.resolve(sourcePath.relativize(file)));
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        return libimobiledeviceFolder;
    }

    private static File getExecutablesFolder() throws IOException {
        File executablesFolder = new File(System.getProperty("user.home"), ".blobsaver_bin");
        if (!executablesFolder.exists()) {
            executablesFolder.mkdir();
            if (PlatformUtil.isWindows()) {
                Files.setAttribute(executablesFolder.toPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
            }
        }
        return executablesFolder;
    }

    /**
     * @param pathToSourceInJar the path to the directory in the jar(ex: "/com/airsquared/blobsaver/libimobiledevice_mac")
     * @param target            where the directory will be copied to
     */
    @SuppressWarnings("Duplicates")
    private static void copyDirFromJar(String pathToSourceInJar, Path target) throws IOException {
        URI resource = null;
        try {
            resource = Shared.class.getResource("").toURI();
        } catch (URISyntaxException e) {
            return;
        }
        if (resource.toString().endsWith("blobsaver.exe!/com/airsquared/blobsaver/")) {
            resource = URI.create(resource.toString().replace("blobsaver.exe!/com/airsquared/blobsaver/", "blobsaver.jar!/com/airsquared/blobsaver/"));
        }
        final java.nio.file.FileSystem fileSystem = FileSystems.newFileSystem(resource, Collections.<String, String>emptyMap());

        final Path jarPath = fileSystem.getPath(pathToSourceInJar);
        System.out.println("jarPath:" + jarPath.toString());
        Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {
            private Path currentTarget;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                currentTarget = target.resolve(jarPath.relativize(dir).toString());
                Files.createDirectories(currentTarget);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path resolveResult = target.resolve(jarPath.relativize(file).toString());
                Files.copy(file, resolveResult, REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
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
