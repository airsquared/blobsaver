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

package airsquared.blobsaver.app.natives;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

public class Libirecovery {

    public static int open(PointerByReference irecv_client) {
        return irecv_open_with_ecid(irecv_client, 0);
    }

    public static int close(Pointer irecv_client) {
        return irecv_close(irecv_client);
    }

    public static int setEnv(Pointer irecv_client, String variable, String value) {
        return irecv_setenv(irecv_client, variable, value);
    }

    public static int saveEnv(Pointer irecv_client) {
        return irecv_saveenv(irecv_client);
    }

    public static int reboot(Pointer irecv_client) {
        return irecv_reboot(irecv_client);
    }

    public static irecv_device_info getDeviceInfo(Pointer irecv_client) {
        return irecv_get_device_info(irecv_client);
    }

    private static native int irecv_open_with_ecid(PointerByReference irecv_client, long ecid);

    private static native int irecv_close(Pointer irecv_client);

    private static native int irecv_setenv(Pointer irecv_client, String variable, String value);

    private static native int irecv_saveenv(Pointer irecv_client);

    private static native int irecv_reboot(Pointer irecv_client);

    private static native irecv_device_info irecv_get_device_info(Pointer irecv_client);

    @SuppressWarnings({"unused", "SpellCheckingInspection"})
    @Structure.FieldOrder({"cpid", "cprv", "cpfm", "scep", "bdid", "ecid", "ibfl", "srnm", "imei", "srtg", "serial_string", "ap_nonce", "ap_nonce_size", "sep_nonce", "sep_nonce_size"})
    public static class irecv_device_info extends Structure {
        public int cpid, cprv, cpfm, scep, bdid;
        public long ecid;
        public int ibfl;
        public String srnm, imei, srtg, serial_string;
        public Pointer ap_nonce;
        public int ap_nonce_size;
        public Pointer sep_nonce;
        public int sep_nonce_size;
    }

    static {
        Native.register(Libirecovery.class, "irecovery");
    }
}
