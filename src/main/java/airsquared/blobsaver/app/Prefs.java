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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Prefs {

    private static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/app");
    private static final Preferences savedDevicesPrefs = appPrefs.node("Saved Devices");
    private static ObservableList<SavedDevice> savedDevicesList;

    public static void resetPrefs() {
        try {
            appPrefs.removeNode();
            appPrefs.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        Analytics.resetPrefs();
    }

    public static void export(File file) {
        try {
            savedDevicesPrefs.exportSubtree(new FileOutputStream(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        Analytics.exportPrefs();
    }

    public static void importXML(File file) throws IOException, InvalidPreferencesFormatException {
        Preferences.importPreferences(new FileInputStream(file));
        if (savedDevicesList != null) {
            savedDevicesList.setAll(savedDevices().toList());
        }
        Analytics.importPrefs();
    }

    public static void importOldVersion() {
        Preferences oldAppPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
        for (int i = 1; i <= 10; i++) {
            Preferences presetPrefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + i);
            if (!presetPrefs.getBoolean("Exists", false)) {
                continue;
            }
            SavedDeviceBuilder builder = new SavedDeviceBuilder(oldAppPrefs.get("Name Preset" + i, "Saved Device " + i));
            builder.setEcid(presetPrefs.get("ECID", null)).setSavePath(presetPrefs.get("Path", null));
            if ("none".equals(presetPrefs.get("Device Model", null))) {
                builder.setIdentifier(presetPrefs.get("Device Identifier", null));
            } else {
                builder.setIdentifier(Devices.modelToIdentifier(presetPrefs.get("Device Model", null)));
            }
            if (!"none".equals(presetPrefs.get("Board Config", null)) && Devices.doesRequireBoardConfig(builder.identifier)) {
                builder.setBoardConfig(presetPrefs.get("Board Config", null));
            }
            if (!Utils.isEmptyOrNull(presetPrefs.get("Apnonce", null))) {
                builder.setApnonce(presetPrefs.get("Apnonce", null));
            }
            builder.save();
        }
    }

    public static void setLastAppVersion(@SuppressWarnings("SameParameterValue") String version) {
        appPrefs.put("App version", version);
    }

    public static void setIgnoreVersion(String ignoreVersion) {
        appPrefs.put("Ignore Version", ignoreVersion);
    }

    public static boolean shouldIgnoreVersion(String testForVersion) {
        return testForVersion.equals(appPrefs.get("Ignore Version", null));
    }

    public static boolean getDisableAnalytics() {
        return appPrefs.getBoolean("Disable Analytics", false);
    }

    public static void setDisableAnalytics(boolean disabled) {
        appPrefs.putBoolean("Disable Analytics", disabled);
        Analytics.disableAnalytics(); // last analytics message that will be sent
    }

    public static String getAnalyticsUUID() {
        return appPrefs.get("Analytics UUID", null);
    }

    public static void setAnalyticsUUID(String uuid) {
        appPrefs.put("Analytics UUID", uuid);
    }

    public static void setShowOldDevices(boolean showOldDevices) {
        appPrefs.putBoolean("Show Old Devices", showOldDevices);
        Analytics.olderDevices(showOldDevices);
    }

    public static boolean getShowOldDevices() {
        return appPrefs.getBoolean("Show Old Devices", false);
    }

    public enum DarkMode {DISABLED, SYNC_WITH_OS, ENABLED}

    public static DarkMode getDarkMode() {
        return DarkMode.valueOf(appPrefs.get("Dark Mode", "DISABLED"));
    }

    public static void setDarkMode(DarkMode darkMode) {
        appPrefs.put("Dark Mode", darkMode.name());
    }

    public static boolean getAlwaysSaveNewBlobs() {
        return appPrefs.getBoolean("Always save new blobs", false);
    }

    public static void setAlwaysSaveNewBlobs(boolean alwaysSaveNewBlobs) {
        appPrefs.putBoolean("Always save new blobs", alwaysSaveNewBlobs);
    }

    private static Stream<SavedDevice> savedDevices() {
        try {
            return Arrays.stream(savedDevicesPrefs.childrenNames()).map(SavedDevice::new);
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return an observable list of the saved devices that is automatically updated
     */
    public static ObservableList<SavedDevice> getSavedDevices() {
        if (savedDevicesList == null) {
            savedDevicesList = savedDevices().collect(Collectors.toCollection(FXCollections::observableArrayList));
        }

        return savedDevicesList;
    }

    public static Stream<SavedDevice> getBackgroundDevices() {
        return savedDevices().filter(SavedDevice::isBackground);
    }

    public static boolean anyBackgroundDevices() {
        return savedDevices().anyMatch(SavedDevice::isBackground);
    }

    public static void setBackgroundInterval(long interval, TimeUnit timeUnit) {
        appPrefs.putLong("Time to run", interval);
        appPrefs.put("Time unit for background", timeUnit.toString());
    }

    public static long getBackgroundInterval() {
        return appPrefs.getLong("Time to run", 1);
    }

    public static TimeUnit getBackgroundTimeUnit() {
        return TimeUnit.valueOf(appPrefs.get("Time unit for background", "DAYS"));
    }

    public static long getBackgroundIntervalMinutes() {
        return getBackgroundTimeUnit().toMinutes(getBackgroundInterval());
    }

    public static class SavedDevice {
        private final Preferences node;

        private SavedDevice(String name) {
            this.node = savedDevicesPrefs.node(name);
        }

        public void delete() {
            try {
                savedDevicesList.remove(this);
                node.removeNode();
            } catch (BackingStoreException e) {
                throw new RuntimeException(e);
            }
        }

        public void rename(String name) { // intentionally different name
            if (getName().equals(name) || Utils.isEmptyOrNull(name)) {
                return;
            }
            new SavedDeviceBuilder(name).setEcid(getEcid()).setSavePath(getSavePath()).setIdentifier(getIdentifier())
                    .setBoardConfig(getBoardConfig().orElse(null)).setApnonce(getApnonce().orElse(null))
                    .setGenerator(getGenerator().orElse(null)).setIncludeBetas(doesIncludeBetas()).save().setBackground(isBackground());
            delete();
        }

        public String getName() {
            return node.name();
        }

        public String getEcid() {
            return node.get("ECID", null);
        }

        public void setEcid(String ecid) {
            node.put("ECID", ecid);
        }

        public String getSavePath() {
            return node.get("Save Path", null);
        }

        public void setSavePath(String savePath) {
            node.put("Save Path", savePath);
        }

        public String getIdentifier() {
            return node.get("Device Identifier", null);
        }

        public void setIdentifier(String identifier) {
            node.put("Device Identifier", identifier);
        }

        public Optional<String> getBoardConfig() {
            return Optional.ofNullable(node.get("Board Config", null));
        }

        public void setBoardConfig(String boardConfig) {
            node.put("Board Config", boardConfig);
        }

        public Optional<String> getApnonce() {
            return Optional.ofNullable(node.get("Apnonce", null));
        }

        public void setApnonce(String apnonce) {
            node.put("Apnonce", apnonce);
        }

        public Optional<String> getGenerator() {
            return Optional.ofNullable(node.get("Generator", null));
        }

        public void setGenerator(String generator) {
            node.put("Generator", generator);
        }

        public boolean doesIncludeBetas() {
            return node.getBoolean("Include Betas", false);
        }

        public void setIncludeBetas(boolean includeBetas) {
            node.putBoolean("Include Betas", includeBetas);
        }

        public boolean isBackground() {
            return node.getBoolean("Save in background", false);
        }

        public void setBackground(boolean background) {
            node.putBoolean("Save in background", background);
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SavedDevice s && this.getName().equals(s.getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class SavedDeviceBuilder {
        private final String name;
        private String ecid, savePath, identifier, boardConfig, apnonce, generator;
        private boolean includeBetas;

        public SavedDeviceBuilder(String name) {
            this.name = name;
        }

        public SavedDeviceBuilder setEcid(String ecid) {
            this.ecid = ecid;
            return this;
        }

        public SavedDeviceBuilder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public SavedDeviceBuilder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public SavedDeviceBuilder setBoardConfig(String boardConfig) {
            this.boardConfig = boardConfig;
            return this;
        }

        public SavedDeviceBuilder setApnonce(String apnonce) {
            this.apnonce = apnonce;
            return this;
        }

        public SavedDeviceBuilder setGenerator(String generator) {
            this.generator = generator;
            return this;
        }

        public SavedDeviceBuilder setIncludeBetas(boolean includeBetas) {
            this.includeBetas = includeBetas;
            return this;
        }

        public SavedDevice save() {
            SavedDevice device = new SavedDevice(Objects.requireNonNull(name, "Device Name"));
            device.setEcid(Objects.requireNonNull(ecid, "ECID"));
            device.setSavePath(Objects.requireNonNull(savePath, "Save Path"));
            device.setIdentifier(Objects.requireNonNull(identifier, "Identifier"));
            device.setIncludeBetas(includeBetas);
            if (boardConfig != null) {
                device.setBoardConfig(boardConfig);
            }
            if (apnonce != null) {
                device.setApnonce(apnonce);
            }
            if (generator != null) {
                device.setGenerator(generator);
            }

            if (!savedDevicesList.contains(device)) {
                savedDevicesList.add(device); // update observable list
            }

            Analytics.saveDevice();
            return device;
        }
    }

}
