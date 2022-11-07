/*
 * Copyright (c) 2021  airsquared
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Devices {

    private static final String[] iPhones = {"iPhone 3G[S]", "iPhone 4 (GSM)",
            "iPhone 4 (GSM 2012)", "iPhone 4 (CDMA)", "iPhone 4[S]", "iPhone 5 (GSM)", "iPhone 5 (Global)",
            "iPhone 5c (GSM)", "iPhone 5c (Global)", "iPhone 5s (GSM)", "iPhone 5s (Global)",
            "iPhone 6+", "iPhone 6", "iPhone 6s", "iPhone 6s+", "iPhone SE", "iPhone 7 (Global)(iPhone9,1)",
            "iPhone 7+ (Global)(iPhone9,2)", "iPhone 7 (GSM)(iPhone9,3)", "iPhone 7+ (GSM)(iPhone9,4)",
            "iPhone 8 (iPhone10,1)", "iPhone 8+ (iPhone10,2)", "iPhone X (iPhone10,3)", "iPhone 8 (iPhone10,4)",
            "iPhone 8+ (iPhone10,5)", "iPhone X (iPhone10,6)", "iPhone XS (Global) (iPhone11,2)",
            "iPhone XS Max (China) (iPhone11,4)", "iPhone XS Max (iPhone11,6)", "iPhone XR (iPhone11,8)",
            "iPhone 11 (iPhone12,1)", "iPhone 11 Pro (iPhone12,3)", "iPhone 11 Pro Max (iPhone12,5)",
            "iPhone SE 2 (iPhone12,8)", "iPhone 12 mini (iPhone13,1)", "iPhone 12 (iPhone13,2)",
            "iPhone 12 Pro (iPhone13,3)", "iPhone 12 Pro Max (iPhone13,4)",
            "iPhone 13 Pro (iPhone14,2)", "iPhone 13 Pro Max (iPhone14,3)",
            "iPhone 13 mini (iPhone14,4)", "iPhone 13 (iPhone14,5)", "iPhone SE 3 (iPhone14,6)", "iPhone 14 (iPhone14,7)",
            "iPhone 14+ (iPhone14,8)", "iPhone 14 Pro (iPhone15,2)", "iPhone 14 Pro Max (iPhone15,3)"};
    private static final String[] iPads = {"iPad 1", "iPad 2 (WiFi)", "iPad 2 (GSM)",
            "iPad 2 (CDMA)", "iPad 2 (Mid 2012)", "iPad Mini (Wifi)", "iPad Mini (GSM)", "iPad Mini (Global)",
            "iPad 3 (WiFi)", "iPad 3 (CDMA)", "iPad 3 (GSM)", "iPad 4 (WiFi)", "iPad 4 (GSM)", "iPad 4 (Global)",
            "iPad Air (Wifi)", "iPad Air (Cellular)", "iPad Air (China)", "iPad Mini 2 (WiFi)", "iPad Mini 2 (Cellular)",
            "iPad Mini 2 (China)", "iPad Mini 3 (WiFi)", "iPad Mini 3 (Cellular)", "iPad Mini 3 (China)",
            "iPad Mini 4 (Wifi)", "iPad Mini 4 (Cellular)", "iPad Air 2 (WiFi)", "iPad Air 2 (Cellular)",
            "iPad Pro 9.7 (Wifi)", "iPad Pro 9.7 (Cellular)", "iPad Pro 12.9 (WiFi)", "iPad Pro 12.9 (Cellular)",
            "iPad 5 (Wifi)", "iPad 5 (Cellular)", "iPad Pro 2 12.9 (WiFi)(iPad7,1)", "iPad Pro 2 12.9 (Cellular)(iPad7,2)",
            "iPad Pro 10.5 (WiFi)(iPad7,3)", "iPad 10.5 (Cellular)(iPad7,4)", "iPad 6 (WiFi)(iPad 7,5)",
            "iPad 6 (Cellular)(iPad7,6)", "iPad 7 (WiFi)(iPad7,11)", "iPad 7 (Cellular)(iPad7,12)",
            "iPad Pro 3 11' (WiFi)(iPad8,1)", "iPad Pro 3 11' (WiFi)(iPad8,2)", "iPad Pro 3 11' (Cellular)(iPad8,3)",
            "iPad Pro 3 11' (Cellular)(iPad8,4)", "iPad Pro 3 12.9'(WiFi)(iPad8,5)", "iPad Pro 3 12.9 (WiFi)(iPad8,6)",
            "iPad Pro 3 12.9 (Cellular)(iPad8,7)", "iPad Pro 3 12.9 (Cellular)(iPad8,8)", "iPad Pro 4 11' (WiFi)(iPad8,9)",
            "iPad Pro 4 11' (Cellular)(iPad8,10)", "iPad Pro 4 12.9' (WiFi)(iPad8,11)",
            "iPad Pro 4 12.9' (Cellular)(iPad8,12)", "iPad Mini 5 (WiFi)(iPad11,1)", "iPad Mini 5 (Cellular)(iPad11,2)",
            "iPad Air 3 (WiFi)(iPad11,3)", "iPad Air 3 (Cellular)(iPad11,4)", "iPad 8 (WiFi) (iPad11,6)",
            "iPad 8 (Cellular) (iPad11,7)", "iPad 9 (WiFi) (iPad12,1)", "iPad 9 (Cellular) (iPad12,2)",
            "iPad Air 4 (WiFi) (iPad13,1)", "iPad Air 4 (Cellular) (iPad13,2)",
            "iPad Pro 11' (3rd gen) (WiFi) (iPad13,4)", "iPad Pro 11' (3rd gen) (WiFi) (iPad13,5)",
            "iPad Pro 11' (3rd gen) (Cellular) (iPad13,6)", "iPad Pro 11' (3rd gen) (Cellular) (iPad13,7)",
            "iPad Pro 12.9' (5th gen) (WiFi) (iPad13,8)", "iPad Pro 12.9' (5th gen) (WiFi) (iPad13,9)",
            "iPad Pro 12.9' (5th gen) (Cellular) (iPad13,10)", "iPad Pro 12.9' (5th gen) (Cellular) (iPad13,11)",
            "iPad Mini 6 (WiFi) (iPad14,1)", "iPad Mini 6 (Cellular) (iPad14,2)", "iPad Air 5 (WiFi) (iPad13,16)",
            "iPad Air 5 (Cellular) (iPad13,17)", "iPad 10 (WiFi) (iPad13,18)", "iPad 10 (Cellular) (iPad13,19)",
            "iPad Pro 11' (4th gen) (WiFi) (iPad14,3)", "iPad Pro 11' (4th gen) (Cellular) (iPad14,4)",
            "iPad Pro 12.9' (6th gen) (WiFi) (iPad14,5)", "iPad Pro 12.9' (6th gen) (Cellular) (iPad14,6)"};

    private static final ObservableList<String> iPhoneList = FXCollections.observableArrayList(iPhones);
    private static final ObservableList<String> iPadList = FXCollections.observableArrayList(iPads);

    private static final ObservableList<String> iPodList = unmodifiableArrayList("iPod Touch 3", "iPod Touch 4",
            "iPod Touch 5", "iPod Touch 6", "iPod Touch 7 (iPod9,1)");

    private static final ObservableList<String> AppleTVList = unmodifiableArrayList("Apple TV 2G", "Apple TV 3",
            "Apple TV 3 (2013)", "Apple TV 4 (2015)", "Apple TV 4K");

    private static final ObservableList<String> deviceTypes = unmodifiableArrayList("iPhone", "iPod", "iPad", "AppleTV");

    private static final Map<String, String> boardConfigs;

    private static final Map<String, String> deviceModelIdentifiers;

    private static final Map<String, String> identifierDeviceModels = new HashMap<>();

    static {
        try {
            deviceModelIdentifiers = loadProperties("devicemodels.properties");
            deviceModelIdentifiers.forEach((k, v) -> identifierDeviceModels.put(v, k));

            boardConfigs = loadProperties("boardconfigs.properties");
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
        } else if (!iPhoneList.get(0).equals("iPhone 6+")) {
            iPhoneList.remove(0, iPhoneList.indexOf("iPhone 6+"));
            iPadList.remove(0, iPadList.indexOf("iPad Mini 4 (Wifi)"));
        }
    }

    public static ObservableList<String> getiPhoneList() {
        return iPhoneList;
    }

    // other accessors not used

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
     * @return either "iPhone", "iPod", "iPad", or "AppleTV"
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
        }
        throw new IllegalArgumentException("Not found: " + identifier);
    }

    public static ObservableList<String> getModelsForType(String deviceType) {
        return switch (deviceType == null ? "" : deviceType) {
            case "iPhone" -> iPhoneList;
            case "iPod" -> iPodList;
            case "iPad" -> iPadList;
            case "AppleTV" -> AppleTVList;
            default -> FXCollections.emptyObservableList();
        };
    }

    public static String getOSNameForType(String deviceType) {
        return switch (deviceType == null ? "" : deviceType) {
            case "iPhone", "iPod" -> "iOS";
            case "iPad" -> "iOS/iPadOS";
            case "AppleTV" -> "tvOS";
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
        return deviceIdentifier.startsWith("iPhone11,") || deviceIdentifier.startsWith("iPhone12,") ||
                deviceIdentifier.startsWith("iPhone13,") || deviceIdentifier.startsWith("iPhone14,") ||
                deviceIdentifier.startsWith("iPad8,") || deviceIdentifier.startsWith("iPad11,") ||
                deviceIdentifier.startsWith("iPad12,") || deviceIdentifier.startsWith("iPad13,") ||
                deviceIdentifier.startsWith("iPad14,");
    }

    @SafeVarargs
    private static <T> ObservableList<T> unmodifiableArrayList(T... items) {
        return FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(items));
    }

    private static Map<String, String> loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        properties.load(Devices.class.getResourceAsStream(resourceName));
        return (Map) properties;
    }
}
