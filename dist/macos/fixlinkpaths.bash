#!/usr/bin/env bash

install_name_tool -change /usr/local/lib/libimobiledevice-glue-1.0.0.dylib @loader_path/libimobiledevice-glue-1.0.0.dylib libusbmuxd.dylib
install_name_tool -change /usr/local/lib/libplist-2.0.4.dylib @loader_path/libplist.dylib libusbmuxd.dylib

install_name_tool -change /usr/local/lib/libimobiledevice-glue-1.0.0.dylib @loader_path/libimobiledevice-glue-1.0.0.dylib libirecovery.dylib
install_name_tool -change /usr/local/lib/libplist-2.0.4.dylib @loader_path/libplist.dylib libirecovery.dylib

install_name_tool -change /usr/local/lib/libplist-2.0.4.dylib @loader_path/libplist.dylib libimobiledevice-glue-1.0.0.dylib

install_name_tool -change /usr/local/lib/libplist-2.0.4.dylib @loader_path/libplist.dylib libimobiledevice.dylib
install_name_tool -change /usr/local/lib/libimobiledevice-glue-1.0.0.dylib @loader_path/libimobiledevice-glue-1.0.0.dylib libimobiledevice.dylib
install_name_tool -change /usr/local/lib/libusbmuxd-2.0.6.dylib @loader_path/libusbmuxd.dylib libimobiledevice.dylib
