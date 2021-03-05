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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.function.Predicate;

class Background {

    private static final String backgroundLabel = "airsquared.blobsaver.BackgroundService";
    private static final Path plistFilePath = Path.of(System.getProperty("user.home"), "Library/LaunchAgents",
            backgroundLabel + ".plist").toAbsolutePath();
    private static final String windowsTaskName = "\\airsquared\\blobsaver\\BackgroundService";


    private static void macosBackgroundFile() {
        String executablePath = Utils.getBlobsaverExecutable().getAbsolutePath();
        long interval = Prefs.getBackgroundTimeUnit().toSeconds(Prefs.getBackgroundInterval());
        //language=XML
        String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">" +
                "<plist>" +
                "<dict>" +
                "<key>Label</key>" +
                "<string>" + backgroundLabel + "</string>" +
                "<key>ProgramArguments</key>" +
                "<array>" +
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
            Files.writeString(plistFilePath, plist);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void linuxBackgroundFile() {
        String executablePath = Utils.getBlobsaverExecutable().getAbsolutePath();
        String service = """
                [Unit]
                Description=Run blobsaver autosave

                [Service]
                Type=oneshot
                ExecStart=%s --background-autosave""".formatted(executablePath);
        String timer = """
                [Unit]
                Description=Run blobsaver autosave
                                
                [Timer]
                OnBootSec=1min
                OnUnitInactiveSec=%dmin
                                
                [Install]
                WantedBy=timers.target
                """.formatted(Prefs.getBackgroundTimeUnit().toMinutes(Prefs.getBackgroundInterval()));
        String dataHome = System.getenv("XDG_DATA_HOME");
        if (dataHome == null) {
            dataHome = System.getProperty("user.home") + "/.local/share";
        }
        Path path = Path.of(dataHome + "/systemd/user");
        try {
            Files.writeString(path.resolve("blobsaver.service"), service);
            Files.writeString(path.resolve("blobsaver.timer"), timer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void startBackground() {
        if (Platform.isMac()) {
            macosBackgroundFile();
            launchctl("load", "-w", plistFilePath.toString());
        } else if (Platform.isWindows()) {
            // https://docs.microsoft.com/en-us/windows-server/administration/windows-commands/schtasks-create
            schtasks("/create", "/it", "/f", "/tn", windowsTaskName,
                    "/tr", Utils.getBlobsaverExecutable().getAbsolutePath() + " --background-autosave",
                    "/sc", "once", "/st", new SimpleDateFormat("HH:mm").format(new Date()),
                    "/ri", Long.toString(Prefs.getBackgroundTimeUnit().toMinutes(Prefs.getBackgroundInterval())));
        } else {
            linuxBackgroundFile();
            systemctl("daemon-reload");
            systemctl("enable", "--user", "--now", "blobsaver.timer");
            runOnce(); // systemd doesn't start it automatically when enabled
        }
    }

    public static void stopBackground() {
        if (Platform.isMac()) {
            launchctl("unload", "-w", plistFilePath.toString());
        } else if (Platform.isWindows()) {
            schtasks("/delete", "/f", "/tn", windowsTaskName);
        } else {
            systemctl("disable", "--user", "--now", "blobsaver.timer");
        }
    }

    public static boolean isBackgroundEnabled() {
        if (Platform.isMac()) {
            return outputMatches(s -> s.contains(backgroundLabel), "/bin/launchctl", "list");
        } else if (Platform.isWindows()) {
            return outputMatches(s -> s.contains("Ready") || s.contains("Running"), "schtasks", "/Query", "/TN", windowsTaskName);
        } else {
            try {
                return new ProcessBuilder("systemctl", "is-enabled", "--user", "--quiet", "blobsaver.timer")
                        .start().waitFor() == 0;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void runOnce() {
        if (Platform.isMac()) {
            launchctl("start", backgroundLabel);
        } else if (Platform.isWindows()) {
            schtasks("/run", windowsTaskName);
        } else {
            systemctl("start", "--user", "blobsaver.service");
        }
    }

    private static void launchctl(String... args) {
        execute("launchctl", args);
    }

    private static void schtasks(String... args) {
        execute("schtasks", args);
    }

    private static void systemctl(String... args) {
        execute("systemctl", args);
    }

    private static void execute(String program, String... args) {
        ArrayList<String> arguments = new ArrayList<>(args.length + 1);
        arguments.add(program);
        Collections.addAll(arguments, args);
        try {
            Utils.executeProgram(arguments);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean outputMatches(Predicate<String> predicate, String... args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ProcessBuilder(args).redirectErrorStream(true).start().getInputStream()))) {
            return reader.lines().anyMatch(predicate);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void saveAllBackgroundBlobs() {
        Prefs.getBackgroundDevices().forEach(Background::saveBlobs);
        System.out.println("Done saving all background blobs");
    }

    private static void saveBlobs(Prefs.SavedDevice savedDevice) {
        System.out.println("attempting to save for device " + savedDevice);

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