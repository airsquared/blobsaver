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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Devices {

    private static final List<String> iPhones = new ArrayList<>(), iPads = new ArrayList<>();

    private static final ObservableList<String> iPhoneList, iPadList,
            iBridgeList = FXCollections.observableArrayList(), macList = FXCollections.observableArrayList();

    private static final ObservableList<String> iPodList = unmodifiableArrayList("iPod Touch 3", "iPod Touch 4",
            "iPod Touch 5", "iPod Touch 6", "iPod Touch 7 (iPod9,1)");

    private static final ObservableList<String> AppleTVList = unmodifiableArrayList("Apple TV 2G", "Apple TV 3",
            "Apple TV 3 (2013)", "Apple TV 4 (2015)", "Apple TV 4K", "Apple TV 4K (2021) (AppleTV11,1)", "Apple TV 4K (2022) (AppleTV14,1)");
    private static final ObservableList<String> visionList = unmodifiableArrayList("Apple Vision Pro (RealityDevice14,1)");

    private static final ObservableList<String> deviceTypes = unmodifiableArrayList("iPhone", "iPod", "iPad",
            "AppleTV", "Mac", "T2 Mac", "Apple Vision");

    private static final Map<String, String> boardConfigs = (Map) new Properties();

    private static final Map<String, String> deviceModelIdentifiers = new HashMap<>(), identifierDeviceModels = new HashMap<>();

    static {
        try {
            try (var stream = Devices.class.getResourceAsStream("boardconfigs.properties")) {
                ((Properties) (Map) boardConfigs).load(stream);
            }

            loadProperties();

            iPadList = FXCollections.observableArrayList(iPads);
            iPhoneList = FXCollections.observableArrayList(iPhones);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        updateLists();
    }

    /**
     * Updates the device lists based on the user preference to show older devices in the UI or not
     */
    public static void updateLists() {
        if (Prefs.getShowOldDevices()) {
            iPhoneList.setAll(iPhones);
            iPadList.setAll(iPads);
        } else if (!iPhoneList.getFirst().equals("iPhone XS (Global) (iPhone11,2)")) {
            iPhoneList.remove(0, iPhoneList.indexOf("iPhone XS (Global) (iPhone11,2)"));
            iPadList.remove(0, iPadList.indexOf("iPad 7 (WiFi)(iPad7,11)"));
        }
    }

    public static ObservableList<String> getiPhoneList() {
        return iPhoneList;
    }

    public static ObservableList<String> getDeviceTypes() {
        return deviceTypes;
    }

    public static boolean containsIdentifier(String identifier) {
        return identifierDeviceModels.containsKey(identifier);
    }

    public static String getBoardConfig(String identifier) {
        return boardConfigs.get(identifier);
    }

    public static String identifierToModel(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifierDeviceModels.get(identifier);
    }

    public static String modelToIdentifier(String deviceModel) {
        if (deviceModel == null) {
            return null;
        }
        return deviceModelIdentifiers.get(deviceModel);
    }

    /**
     * @return either "iPhone", "iPod", "iPad", "AppleTV", "Mac",  "T2 Mac".
     */
    public static String getDeviceType(String identifier) {
        if (identifier.startsWith("iPhone")) {
            return "iPhone";
        } else if (identifier.startsWith("iPod")) {
            return "iPod";
        } else if (identifier.startsWith("iPad")) {
            return "iPad";
        } else if (identifier.startsWith("AppleTV")) {
            return "AppleTV";
        } else if (identifier.startsWith("Mac")) {
            return "Mac";
        } else if (identifier.startsWith("iBridge")) {
            return "T2 Mac";
        } else if (identifier.startsWith("RealityDevice")) {
            return "Apple Vision";
        }
        throw new IllegalArgumentException("Not found: " + identifier);
    }

    public static ObservableList<String> getModelsForType(String deviceType) {
        return switch (deviceType == null ? "" : deviceType) {
            case "iPhone" -> iPhoneList;
            case "iPod" -> iPodList;
            case "iPad" -> iPadList;
            case "AppleTV" -> AppleTVList;
            case "Mac" -> macList;
            case "T2 Mac" -> iBridgeList;
            case "Apple Vision" -> visionList;
            default -> FXCollections.emptyObservableList();
        };
    }

    public static String getOSNameForType(String deviceType) {
        return switch (deviceType == null ? "" : deviceType) {
            case "iPhone", "iPod" -> "iOS";
            case "iPad" -> "iOS/iPadOS";
            case "AppleTV" -> "tvOS";
            case "Mac" -> "macOS";
            case "T2 Mac" -> "bridgeOS";
            case "Apple Vision" -> "visionOS";
            default -> null;
        };
    }

    public static String getOSNameForIdentifier(String identifier) {
        return getOSNameForType(getDeviceType(identifier));
    }

    public static boolean doesRequireBoardConfig(String deviceIdentifier) {
        return !boardConfigs.containsKey(deviceIdentifier);
    }

    public static boolean doesRequireApnonce(String deviceIdentifier) {
        String model = identifierToModel(deviceIdentifier);
        if (deviceIdentifier.startsWith("iPhone")) {
            return iPhoneList.indexOf(model) >= iPhoneList.indexOf(identifierToModel("iPhone11,2"));
        } else if (deviceIdentifier.startsWith("iPad")) {
            return iPadList.indexOf(model) >= iPadList.indexOf((identifierToModel("iPad8,1")));
        } else {
            return false;
        }
    }

    @SafeVarargs
    private static <T> ObservableList<T> unmodifiableArrayList(T... items) {
        return FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(items));
    }

    private static void loadProperties() throws IOException {
        var loader = new Properties() {
            private Consumer<String> keyProcessor;

            void load(String resourceName, Consumer<String> keyProcessor) throws IOException {
                this.keyProcessor = keyProcessor; // not thread safe
                try (var stream = Devices.class.getResourceAsStream(resourceName)) {
                    load(stream);
                }
            }

            @Override
            public Object put(Object key, Object value) {
                deviceModelIdentifiers.put((String) key, (String) value);
                identifierDeviceModels.put((String) value, (String) key);
                keyProcessor.accept((String) key);
                return null;
            }
        };

        loader.load("devicemodels/iPhones.properties", iPhones::add);
        loader.load("devicemodels/iPads.properties", iPads::add);
        loader.load("devicemodels/Macs.properties", macList::add);
        loader.load("devicemodels/iBridges.properties", iBridgeList::add);
        loader.load("devicemodels/others.properties", _ -> {});
    }
}
