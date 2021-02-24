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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

class Prefs {

    private static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/app");
    private static final Preferences savedDevicesPrefs = appPrefs.node("Saved Devices");

    public static void setLastAppVersion(@SuppressWarnings("SameParameterValue") String version) {
        appPrefs.put("App version", version);
    }

    public static void setIgnoreVersion(String ignoreVersion) {
        appPrefs.put("Ignore Version", ignoreVersion);
    }

    public static boolean shouldIgnoreVersion(String testForVersion) {
        return testForVersion.equals(appPrefs.get("Ignore Version", null));
    }

    public static void resetPrefs() throws BackingStoreException {
        appPrefs.removeNode();
        appPrefs.flush();
    }

    public static Optional<SavedDevice> savedDevice(int num) {
        if (savedDeviceExists(num)) {
            return Optional.of(new SavedDevice(num));
        } else {
            return Optional.empty();
        }
    }

    private static boolean savedDeviceExists(int num) {
        try {
            return savedDevicesPrefs.nodeExists(Integer.toString(num));
        } catch (BackingStoreException e) {
            return false;
        }
    }

    public static String getSavedDeviceName(int num) {
        return savedDevice(num).map(SavedDevice::getName).orElse("Device " + num);
    }

    public static Stream<SavedDevice> getSavedDevices() {
        try {
            return Arrays.stream(savedDevicesPrefs.childrenNames()).map(SavedDevice::new);
        } catch (BackingStoreException e) {
            throw new RuntimeException(e); // TODO: handle this
        }
    }

    public static Stream<SavedDevice> getBackgroundDevices() {
        return getSavedDevices().filter(SavedDevice::isBackground);
    }

    public static boolean anyBackgroundDevices() {
        return getSavedDevices().anyMatch(SavedDevice::isBackground);
    }

    public static boolean isDeviceInBackground(int num) {
        return savedDevice(num).map(SavedDevice::isBackground).orElse(false);
    }

    public static void setBackgroundInterval(long interval, TimeUnit timeUnit) {
        appPrefs.putLong("Time to run", interval);
        appPrefs.put("Time unit for background", timeUnit.toString());
    }

    public static long getBackgroundInterval() {
        return appPrefs.getLong("Time to run", 1);
    }

    public static TimeUnit getBackgroundIntervalTimeUnit() {
        return TimeUnit.valueOf(appPrefs.get("Time unit for background", "DAYS"));
    }

    public static class SavedDevice {
        public final int number;
        public final Preferences node;

        private SavedDevice(int number) {
            this.number = number;
            this.node = savedDevicesPrefs.node(Integer.toString(number));
        }

        private SavedDevice(String number) {
            this.number = Integer.parseInt(number);
            this.node = savedDevicesPrefs.node(number);
        }

        public String getName() {
            return node.get("Name", null);
        }

        public void setName(String name) {
            node.put("Name", name);
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

        public boolean isBackground() {
            return node.getBoolean("Save in background", false);
        }

        public void setBackground(boolean background) {
            node.putBoolean("Save in background", background);
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    public static class SavedDeviceBuilder {
        private final int number;
        private String name, ecid, savePath, identifier, boardConfig, apnonce;

        public SavedDeviceBuilder(int number) {
            this.number = number;
        }

        public SavedDeviceBuilder setName(String name) {
            this.name = name;
            return this;
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

        public SavedDevice save() {
            SavedDevice device = new SavedDevice(number);
            device.setName(Objects.requireNonNull(name, "Device Name"));
            device.setEcid(Objects.requireNonNull(ecid, "ECID"));
            device.setSavePath(Objects.requireNonNull(savePath, "Save Path"));
            device.setIdentifier(Objects.requireNonNull(identifier, "Identifier"));
            if (boardConfig != null) {
                device.setBoardConfig(boardConfig);
            }
            if (apnonce != null) {
                device.setApnonce(apnonce);
            }
            return device;
        }
    }

}
