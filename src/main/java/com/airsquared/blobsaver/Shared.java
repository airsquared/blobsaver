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

import com.airsquared.blobsaver.model.Version;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static com.airsquared.blobsaver.Main.appPrefs;
import static com.airsquared.blobsaver.Main.appVersion;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

// code shared by Controller and Background
@SuppressWarnings("ResultOfMethodCallIgnored")
class Shared {

    static ButtonType redditPM = new ButtonType("PM on Reddit");
    static ButtonType githubIssue = new ButtonType("Create Issue on Github");

    static String textToIdentifier(String deviceModel) {
        String toReturn = Devices.getDeviceModelIdentifiersMap().getOrDefault(deviceModel, "");
        if ("".equals(toReturn)) { // this will never happen in background
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not find: \"" + deviceModel + "\"" + "\n\nPlease create a new issue on Github or PM me on Reddit.", new ButtonType("Create Issue on Github"), new ButtonType("PM on Reddit"), ButtonType.CANCEL);
            resizeAlertButtons(alert);
            alert.showAndWait();
            if (alert.getResult().equals(new ButtonType("Create Issue on Github"))) {
                openURL("https://github.com/airsquared/blobsaver/issues/new/choose");
            } else if (alert.getResult().equals(new ButtonType("PM on Reddit"))) {
                openURL("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report");
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
                        String response;
                        try {
                            response = makeRequest(new URL("https://api.github.com/repos/airsquared/blobsaver/releases/latest"));
                        } catch (FileNotFoundException e) {
                            System.err.println("https://api.github.com/repos/airsquared/blobsaver/releases/latest " +
                                    "doesn't exist??");
                            e.printStackTrace();
                            return null;
                        } catch (IOException e) {
                            System.err.println("unknown IOException while checking for updates :( " +
                                    "Are you connected to the internet?");
                            e.printStackTrace();
                            return null;
                        }
                        Version newVersion;
                        String changelog;
                        try {
                            newVersion = new Version(new JSONObject(response).getString("tag_name"));
                            changelog = new JSONObject(response).getString("body");
                            changelog = changelog.substring(changelog.indexOf("Changelog"));
                        } catch (JSONException e) {
                            newVersion = appVersion;
                            changelog = "";
                        }
                        if (newVersion.compareTo(appVersion) < 0 //check if this is >= latest version
                                && (forceCheck || !appPrefs.get("Ignore Version", "").equals(newVersion.toString()))) {
                            final CountDownLatch latch = new CountDownLatch(1);
                            final String finalNewVersion = newVersion.toString(); //so that the lambda works
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
                                        openURL("https://github.com/airsquared/blobsaver/releases/latest");
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

    static String makeRequest(URL url) throws IOException {
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

    static File getTsschecker() throws IOException {
        File executablesFolder = getExecutablesFolder();
        File tsschecker = new File(executablesFolder, "tsschecker");
        if (tsschecker.exists() && appPrefs.getBoolean("tsschecker last update v2.2.3", false)) {
            return tsschecker;
        } else {
            if (tsschecker.exists()) {
                tsschecker.delete();
            }
            InputStream input;
            if (PlatformUtil.isMac()) {
                input = Shared.class.getResourceAsStream("tsschecker_macos");
            } else if (PlatformUtil.isWindows()) {
                input = Shared.class.getResourceAsStream("tsschecker_windows.exe");
            } else {
                input = Shared.class.getResourceAsStream("tsschecker_linux");
            }
            tsschecker.createNewFile();
            copyStreamToFile(input, tsschecker);
            tsschecker.setReadable(true, false);
            tsschecker.setExecutable(true, false);
            appPrefs.putBoolean("tsschecker last update v2.2.3", true);
            return tsschecker;
        }
    }

    static File getlibimobiledeviceFolder() throws IOException {
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
        URI resource;
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
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path currentTarget = target.resolve(jarPath.relativize(dir).toString());
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

    static void copyStreamToFile(InputStream inputStream, File file) throws IOException {
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
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static String executeProgram(String... command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder logBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logBuilder.append(line).append("\n");
            }
            return logBuilder.toString();
        }
    }

    static String getJarLocation() {
        final String url = Shared.class.getResource("Shared.class").toString();
        String path = url.substring(0, url.length() - "com/airsquared/blobsaver/Controller.class".length());
        if (path.startsWith("jar:")) {
            path = path.substring("jar:".length(), path.length() - 2);
        }
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        return path;
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

    static void runSafe(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
