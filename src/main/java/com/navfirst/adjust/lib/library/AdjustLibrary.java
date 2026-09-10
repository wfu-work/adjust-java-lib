package com.navfirst.adjust.lib.library;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * 平差 C ABI，直接将 UTF-8 返回值映射为 String。
 */
public interface AdjustLibrary extends Library {

    String ADJUST_NAME = "Adjust";

    AdjustLibrary INSTANCE = Native.load(ADJUST_NAME, AdjustLibrary.class);

    AdjustLibrary INSTANTCES = (AdjustLibrary) Native.synchronizedLibrary(
            Native.load(ADJUST_NAME, AdjustLibrary.class)
    );

    String getVersion();

    String startAdjust(String requestJson);

    String startAdjustWgs84(String requestJson);
}
