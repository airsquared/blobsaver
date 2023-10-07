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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

final class Analytics {

    private static final String trackingID = "UA-197702248-2";

    public static void startup() {
        collect("/");
    }

    public static void saveBlobs() {
        collect("/save-blobs");
    }

    public static void saveDevice() {
        collect("/save-device");
    }

    public static void startBackground() {
        collect("/start-background");
    }

    public static void readInfo() {
        collect("/read/info");
    }

    public static void readAPNonce(boolean jailbroken) {
        collect("/read/apnonce/" + (jailbroken ? "jailbroken" : "unjailbroken"));
    }

    public static void olderDevices(boolean show) {
        collect("/older-devices/" + (show ? "show" : "hide"));
    }

    public static void checkBlobs() {
        collect("/check-blobs");
    }

    public static void exitRecovery() {
        collect("/exit-recovery");
    }

    public static void importPrefs() {
        collect("/prefs/import");
    }

    public static void exportPrefs() {
        collect("/prefs/export");
    }

    public static void resetPrefs() {
        collect("/clear-app-data");
    }

    public static void disableAnalytics() {
        collect("/disable-analytics");
    }

    private static void collect(String page) {
        if (Prefs.getDisableAnalytics()) {
            return;
        }
        sendRequest(STR."\{getBaseUrl()}&t=pageview&dl=\{encode(page)}&an=blobsaver&av=\{encode(Main.appVersion)}&ul=\{encode(Locale.getDefault().toLanguageTag())}");
    }

    private static String getBaseUrl() {
        return STR."http://www.google-analytics.com/collect?v=1&aip=1&ds=app&tid=\{trackingID}&cid=\{getUUID()}";
    }

    private static String getUUID() {
        if (System.getenv().containsKey("GITHUB_ACTIONS")) {
            return "GITHUB_ACTIONS";
        }
        if (Prefs.getAnalyticsUUID() == null) {
            Prefs.setAnalyticsUUID(UUID.randomUUID().toString());
        }
        return Prefs.getAnalyticsUUID();
    }

    private static void sendRequest(String url) {
        try {
            Network.makeVoidRequest(url);
        } catch (Throwable e) { // don't interrupt application if error occurs
            System.err.println(e);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

}
