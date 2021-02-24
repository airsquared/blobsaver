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

import com.sun.jna.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

class Background {

    private static final String backgroundLabel = "airsquared.blobsaver.app.BackgroundService";

    private static final String plistFilePath = System.getProperty("user.home")
            + "/Library/LaunchAgents/" + backgroundLabel + ".plist";


    private static void macosBackgroundFile() {
//        String executablePath = Utils.getBlobsaverExecutable().getAbsolutePath();
        String java = System.getProperty("java.home") + "/bin/java";
        String executablePath = new File(Main.jarDirectory, "blobsaver.jar").getAbsolutePath();
        long interval = Prefs.getBackgroundIntervalTimeUnit().toSeconds(Prefs.getBackgroundInterval());
        // eventually replace with Java 15 text blocks
        //language=XML
        String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">" +
                "<plist>" +
                "<dict>" +
                "<key>Label</key>" +
                "<string>airsquared.blobsaver.app.BackgroundService</string>" +
                "<key>ProgramArguments</key>" +
                "<array>" +
                "  <string>" + java + "</string>" +
                "  <string>-jar</string>" +
                "  <string>" + executablePath + "</string>" +
                "  <string>--background-autosave</string>" +
                "</array>" +
                "<key>RunAtLoad</key>" +
                "<true/>" +
                "<key>StartInterval</key>" +
                "<integer>" + interval + "</integer>" +
                "</dict>" +
                "</plist>";
        try {
            Files.write(Paths.get(plistFilePath), plist.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void startBackground() {
        // TODO: throw exception and show to user if background is not available
        if (Platform.isMac()) {
            macosBackgroundFile();
            launchctl("load", "-w", plistFilePath);
        } else if (Platform.isWindows()) {
            // TODO: schtasks
        } else {
            // TODO: systemd timers
        }
    }

    public static void stopBackground() {
        launchctl("unload", "-w", plistFilePath);
    }

    public static boolean isBackgroundEnabled() {
        // don't use Utils.executeProgram() because don't need to print any output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ProcessBuilder("/bin/launchctl", "list").start().getInputStream()))) {
            return reader.lines().anyMatch(s -> s.contains(backgroundLabel));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void launchctl(String... args) {
        ArrayList<String> arguments = new ArrayList<>(args.length + 1);
        arguments.add("/bin/launchctl");
        Collections.addAll(arguments, args);
        try {
            Utils.executeProgram(arguments);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void runOnce() {
        launchctl("start", backgroundLabel);
    }

    public static void saveAllBackgroundBlobs() {
        Prefs.getBackgroundDevices().forEach(Background::saveBlobs);
        System.out.println("Done saving all background blobs");
    }

    private static void saveBlobs(Prefs.SavedDevice savedDevice) {
        System.out.println("attempting to save for device " + savedDevice.number);

        TSS.Builder builder = new TSS.Builder().setDevice(savedDevice.getIdentifier())
                .setEcid(savedDevice.getEcid()).setSavePath(savedDevice.getSavePath());
        savedDevice.getBoardConfig().ifPresent(builder::setBoardConfig);
        savedDevice.getApnonce().ifPresent(builder::setApnonce);

        try {
            builder.build().call();
            // TODO: show a notification
        } catch (Throwable t) {
            t.printStackTrace();
            // TODO: log it or show a notification
        }
    }

}