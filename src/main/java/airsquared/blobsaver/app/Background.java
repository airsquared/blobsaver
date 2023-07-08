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

import com.sun.jna.Platform;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

class Background {

    private static final String backgroundLabel = "airsquared.blobsaver.BackgroundService";
    private static final Path plistFilePath = Platform.isMac() ?
            Path.of(System.getProperty("user.home"), "Library/LaunchAgents", backgroundLabel + ".plist")
                    .toAbsolutePath()
            : null;

    private static final Path systemdDir;
    static {
        if (!Platform.isMac() && !Platform.isWindows()) {
            String dataHome = System.getenv("XDG_DATA_HOME");
            if (dataHome == null) {
                dataHome = System.getProperty("user.home") + "/.local/share";
            }
            systemdDir = Path.of(dataHome + "/systemd/user");
        } else systemdDir = null;
    }

    private static final String windowsTaskName = "\\airsquared\\blobsaver\\BackgroundService";


    private static void macosBackgroundFile() {
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
                "  <string>" + executablePath() + "</string>" +
                "  <string>--background-autosave</string>" +
                "</array>" +
                "<key>RunAtLoad</key>" +
                "<true/>" +
                "<key>StartInterval</key>" +
                "<integer>" + interval + "</integer>" +
                "</dict>" +
                "</plist>";
        try {
            Files.createDirectories(plistFilePath.getParent());
            Files.writeString(plistFilePath, plist);
            System.out.println("Wrote to: " + plistFilePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void linuxBackgroundFile() {
        String service = """
                [Unit]
                Description=Save blobs in the background

                [Service]
                Type=oneshot
                ExecStart=%s --background-autosave""".formatted(executablePath());
        String timer = """
                [Unit]
                Description=Save blobs in the background
                                
                [Timer]
                OnBootSec=1min
                OnUnitInactiveSec=%dmin
                                
                [Install]
                WantedBy=timers.target
                """.formatted(Prefs.getBackgroundIntervalMinutes());
        try {
            Files.createDirectories(systemdDir);
            Files.writeString(systemdDir.resolve("blobsaver.service"), service);
            Files.writeString(systemdDir.resolve("blobsaver.timer"), timer);
            System.out.println("Wrote to: " + systemdDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path windowsBackgroundFile() {
        String xml = """
                <?xml version="1.0" encoding="UTF-16"?>
                <Task version="1.4" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task">
                  <RegistrationInfo>
                    <Author>airsquared</Author>
                    <Description>Save blobs in the background</Description>
                    <URI>%1$s</URI>
                  </RegistrationInfo>
                  <Triggers>
                    <LogonTrigger>
                      <Repetition>
                        <Interval>PT%2$dM</Interval>
                        <StopAtDurationEnd>false</StopAtDurationEnd>
                      </Repetition>
                      <Enabled>true</Enabled>
                      <UserId>%3$s</UserId>
                    </LogonTrigger>
                    <RegistrationTrigger>
                      <Repetition>
                        <Interval>PT%2$dM</Interval>
                        <StopAtDurationEnd>false</StopAtDurationEnd>
                      </Repetition>
                      <Enabled>true</Enabled>
                    </RegistrationTrigger>
                  </Triggers>
                  <Principals>
                    <Principal id="Author">
                      <UserId>%3$s</UserId>
                      <LogonType>InteractiveToken</LogonType>
                      <RunLevel>LeastPrivilege</RunLevel>
                    </Principal>
                  </Principals>
                  <Settings>
                    <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>
                    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
                    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
                    <AllowHardTerminate>true</AllowHardTerminate>
                    <StartWhenAvailable>true</StartWhenAvailable>
                    <RunOnlyIfNetworkAvailable>true</RunOnlyIfNetworkAvailable>
                    <IdleSettings>
                      <StopOnIdleEnd>false</StopOnIdleEnd>
                      <RestartOnIdle>false</RestartOnIdle>
                    </IdleSettings>
                    <AllowStartOnDemand>true</AllowStartOnDemand>
                    <Enabled>true</Enabled>
                    <Hidden>false</Hidden>
                    <RunOnlyIfIdle>false</RunOnlyIfIdle>
                    <DisallowStartOnRemoteAppSession>false</DisallowStartOnRemoteAppSession>
                    <UseUnifiedSchedulingEngine>true</UseUnifiedSchedulingEngine>
                    <WakeToRun>false</WakeToRun>
                    <ExecutionTimeLimit>PT0S</ExecutionTimeLimit>
                    <Priority>7</Priority>
                  </Settings>
                  <Actions Context="Author">
                    <Exec>
                      <Command>"%4$s"</Command>
                      <Arguments>--background-autosave</Arguments>
                    </Exec>
                  </Actions>
                </Task>
                """.formatted(windowsTaskName, Prefs.getBackgroundIntervalMinutes(), System.getProperty("user.name"), executablePath());
        try {
            Path path = Files.createTempFile("blobsaver_background_service", ".xml");
            Files.writeString(path, xml);
            System.out.println("Wrote to: " + path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public static void startBackground() {
        if (Platform.isMac()) {
            macosBackgroundFile();
            launchctl("load", "-w", plistFilePath.toString());
        } else if (Platform.isWindows()) {
            schtasks("/create", "/xml", windowsBackgroundFile().toString(), "/f", "/tn", windowsTaskName);
        } else {
            linuxBackgroundFile();
            systemctl("daemon-reload");
            systemctl("enable", "--user", "--now", "blobsaver.timer");
            runOnce(); // systemd doesn't start it automatically when enabled
        }
        Analytics.startBackground();
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
            schtasks("/run", "/tn", windowsTaskName);
        } else {
            systemctl("start", "--user", "blobsaver.service");
        }
    }

    public static void deleteBackgroundFile() throws IOException {
        if (Platform.isMac()) {
            Files.deleteIfExists(plistFilePath);
        } else if (!Platform.isWindows()) {
            Files.deleteIfExists(systemdDir.resolve("blobsaver.service"));
            Files.deleteIfExists(systemdDir.resolve("blobsaver.timer"));
        }
    }

    private static String executablePath() {
        return Utils.getBlobsaverExecutable().getAbsolutePath();
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
        try (var reader = new ProcessBuilder(args).redirectErrorStream(true).start().inputReader()) {
            return reader.lines().anyMatch(predicate);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void saveAllBackgroundBlobs() {
        Prefs.getBackgroundDevices().forEach(savedDevice -> {
            System.out.println("attempting to save for device " + savedDevice);

            saveBlobs(savedDevice);
        });
        System.out.println("Done saving all background blobs");
    }

    public static void saveBlobs(Prefs.SavedDevice savedDevice) {
        TSS.Builder builder = new TSS.Builder().setDevice(savedDevice.getIdentifier())
                .setEcid(savedDevice.getEcid()).setSavePath(savedDevice.getSavePath())
                .setIncludeBetas(savedDevice.doesIncludeBetas());
        savedDevice.getBoardConfig().ifPresent(builder::setBoardConfig);
        savedDevice.getApnonce().ifPresent(builder::setApnonce);
        savedDevice.getGenerator().ifPresent(builder::setGenerator);

        try {
            builder.build().call();
            // TODO: show a notification
        } catch (Throwable t) {
            t.printStackTrace();
            // TODO: log it or show a notification
        }
    }

}