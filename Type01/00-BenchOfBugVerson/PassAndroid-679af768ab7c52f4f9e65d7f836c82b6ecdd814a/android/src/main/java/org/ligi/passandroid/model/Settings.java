package org.ligi.passandroid.model;

import java.io.File;
import org.ligi.passandroid.model.comparator.PassSortOrder;

public interface Settings {

    PassSortOrder getSortOrder();

    boolean doTraceDroidEmailSend();

    File getPassesDir();

    File getStateDir();

    File getShareDir();

    boolean isCondensedModeEnabled();

    boolean isAutomaticLightEnabled();

}
