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
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

public class Prefs {

    static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");

    private static final Preferences savedDevicesPrefs = appPrefs.node("Saved Devices");

    static void resetPrefs() throws BackingStoreException {
        appPrefs.clear();
        appPrefs.removeNode();
        appPrefs.flush();
    }

    static Optional<SavedDevice> savedDevice(int num) {
        if (savedDeviceExists(num)) {
            return Optional.of(new SavedDevice(num));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the saved device, creating a new one if necessary
     */
    static SavedDevice createSavedDevice(int num) {
        return new SavedDevice(num);
    }

    static boolean savedDeviceExists(int num) {
        try {
            return savedDevicesPrefs.nodeExists(Integer.toString(num));
        } catch (BackingStoreException e) {
            return false;
        }
    }

    static Stream<SavedDevice> getSavedDevices() {
        try {
            return Arrays.stream(savedDevicesPrefs.childrenNames()).map(SavedDevice::new);
        } catch (BackingStoreException e) {
            throw new RuntimeException(e); // TODO: handle this
        }
    }

    static Stream<SavedDevice> getBackgroundDevices() {
        return getSavedDevices().filter(SavedDevice::isBackground);
    }

    static boolean anyBackgroundDevices() {
        return getSavedDevices().anyMatch(SavedDevice::isBackground);
    }

    static boolean isDeviceInBackground(int num) {
        return savedDevice(num).map(SavedDevice::isBackground).orElse(false);
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
            return node.get("Name", "Device " + number);
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

}
