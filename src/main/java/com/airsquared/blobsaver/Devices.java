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

package com.airsquared.blobsaver;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Devices {

    private static final ObservableList<String> iPhones = FXCollections.observableArrayList("iPhone 3G[S]", "iPhone 4 (GSM)",
            "iPhone 4 (GSM 2012)", "iPhone 4 (CDMA)", "iPhone 4[S]", "iPhone 5 (GSM)", "iPhone 5 (Global)",
            "iPhone 5c (GSM)", "iPhone 5c (Global)", "iPhone 5s (GSM)", "iPhone 5s (Global)",
            "iPhone 6+", "iPhone 6", "iPhone 6s", "iPhone 6s+", "iPhone SE", "iPhone 7 (Global)(iPhone9,1)",
            "iPhone 7+ (Global)(iPhone9,2)", "iPhone 7 (GSM)(iPhone9,3)", "iPhone 7+ (GSM)(iPhone9,4)",
            "iPhone 8 (iPhone10,1)", "iPhone 8+ (iPhone10,2)", "iPhone X (iPhone10,3)", "iPhone 8 (iPhone10,4)",
            "iPhone 8+ (iPhone10,5)", "iPhone X (iPhone10,6)", "iPhone XS (Global) (iPhone11,2)",
            "iPhone XS Max (China) (iPhone11,4)", "iPhone XS Max (iPhone11,6)", "iPhone XR (iPhone11,8)",
            "iPhone 11 (iPhone12,1)", "iPhone 11 Pro (iPhone12,3)", "iPhone 11 Pro Max (iPhone12,5)",
            "iPhone SE 2 (iPhone12,8)");

    private static final ObservableList<String> iPods =
            FXCollections.observableArrayList("iPod Touch 3", "iPod Touch 4", "iPod Touch 5", "iPod Touch 6",
                    "iPod Touch 7 (iPod9,1)");

    private static final ObservableList<String> iPads =
            FXCollections.observableArrayList("iPad 1", "iPad 2 (WiFi)", "iPad 2 (GSM)",
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
                    "iPad Air 3 (WiFi)(iPad11,3)", "iPad Air 3 (Cellular)(iPad11,4)");

    private static final ObservableList<String> AppleTVs =
            FXCollections.observableArrayList("Apple TV 2G", "Apple TV 3", "Apple TV 3 (2013)", "Apple TV 4 (2015)", "Apple TV 4K");

    private static final ObservableList<String> deviceTypes = FXCollections.observableArrayList("iPhone", "iPod", "iPad", "AppleTV");

    private static final HashMap<String, String> requiresBoardConfig = new HashMap<>();

    private static HashMap<String, String> deviceModelIdentifiers = null;

    static {
        requiresBoardConfig.put("iPhone12,8", "D79AP");
        requiresBoardConfig.put("iPad8,9", "J417AP");
        requiresBoardConfig.put("iPad8,10", "J418AP");
        requiresBoardConfig.put("iPad8,11", "J420AP");
        requiresBoardConfig.put("iPad8,12", "J421AP");

        // devices with multiple board configs
        requiresBoardConfig.put("iPhone8,1", "");
        requiresBoardConfig.put("iPhone8,2", "");
        requiresBoardConfig.put("iPhone8,4", "");
        requiresBoardConfig.put("iPad6,11", "");
        requiresBoardConfig.put("iPad6,12", "");
    }

    static ObservableList<String> getiPhones() {
        return iPhones;
    }

    static ObservableList<String> getiPods() {
        return iPods;
    }

    static ObservableList<String> getiPads() {
        return iPads;
    }

    static ObservableList<String> getAppleTVs() {
        return AppleTVs;
    }

    public static ObservableList<String> getDeviceTypes() {
        return deviceTypes;
    }

    static HashMap<String, String> getRequiresBoardConfigMap() {
        return requiresBoardConfig;
    }

    static HashMap<String, String> getDeviceModelIdentifiersMap() {
        if (deviceModelIdentifiers == null) {
            try {
                Properties properties = new Properties();
                properties.load(Shared.class.getResourceAsStream("devicemodels.properties"));
                @SuppressWarnings("unchecked") Map<String, String> prop = ((Map) properties);
                deviceModelIdentifiers = new HashMap<>(prop);
                prop.forEach((key, value) -> deviceModelIdentifiers.put(value, key));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return deviceModelIdentifiers;
    }
}
