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

package airsquared.blobsaver.app.natives;

import airsquared.blobsaver.app.natives.NativeUtils.CFunctionName;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

/**
 * https://github.com/libimobiledevice/libirecovery/blob/master/src/libirecovery.c
 */
public class Libirecovery {

    public static int open(PointerByReference irecv_client) {
        return irecv_open_with_ecid(irecv_client, 0);
    }

    private static native int irecv_open_with_ecid(PointerByReference irecv_client, long ecid);

    @CFunctionName("irecv_close")
    public static native int close(Pointer irecv_client);

    @CFunctionName("irecv_setenv")
    public static native int setEnv(Pointer irecv_client, String variable, String value);

    @CFunctionName("irecv_saveenv")
    public static native int saveEnv(Pointer irecv_client);

    @CFunctionName("irecv_reboot")
    public static native int reboot(Pointer irecv_client);

    @CFunctionName("irecv_send_command")
    public static native int sendCommand(Pointer irecv_client, String command);

    @CFunctionName("irecv_get_device_info")
    public static native irecv_device_info getDeviceInfo(Pointer irecv_client);

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
        try {
            NativeUtils.register(Libirecovery.class, "irecovery");
        } catch (UnsatisfiedLinkError e) {
            try {
                NativeUtils.register(Libirecovery.class, "irecovery-1.0");
            } catch (UnsatisfiedLinkError e2) {
                e.addSuppressed(e2);
                throw e;
            }
        }
    }
}
