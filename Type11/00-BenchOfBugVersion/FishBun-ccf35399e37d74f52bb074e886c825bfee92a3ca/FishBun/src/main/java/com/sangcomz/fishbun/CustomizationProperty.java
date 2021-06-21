package com.sangcomz.fishbun;

import android.graphics.drawable.Drawable;

/**
 * Created by sangcomz on 13/05/2017.
 */

interface CustomizationProperty {
    FishBunCreator setAlbumThumbnailSize(int size);

    FishBunCreator setPickerSpanCount(int spanCount);

    FishBunCreator setActionBarColor(int actionbarColor);

    FishBunCreator setActionBarTitleColor(int actionbarTitleColor);

    FishBunCreator setActionBarColor(int actionbarColor, int statusBarColor);

    FishBunCreator setActionBarColor(int actionbarColor, int statusBarColor, boolean isStatusBarLight);

    FishBunCreator setCamera(boolean isCamera);

    FishBunCreator textOnNothingSelected(String message);

    FishBunCreator textOnImagesSelectionLimitReached(String message);

    FishBunCreator setButtonInAlbumActivity(boolean isButton);

    FishBunCreator setAlbumSpanCount(int portraitSpanCount, int landscapeSpanCount);

    FishBunCreator setAlbumSpanCountOnlyLandscape(int landscapeSpanCount);

    FishBunCreator setAlbumSpanCountOnlPortrait(int portraitSpanCount);

    FishBunCreator setAllViewTitle(String allViewTitle);

    FishBunCreator setActionBarTitle(String actionBarTitle);

    FishBunCreator setHomeAsUpIndicatorDrawable(Drawable icon);

    FishBunCreator setDoneButtonDrawable(Drawable icon);

    FishBunCreator setAllDoneButtonDrawable(Drawable icon);

    FishBunCreator setIsUseAllDoneButton(Boolean isUse);

    FishBunCreator setMenuDoneText(String text);

    FishBunCreator setMenuAllDoneText(String text);

    FishBunCreator setMenuTextColor(int color);

    FishBunCreator setIsUseDetailView(boolean isUse);

    FishBunCreator setIsShowCount(boolean isShow);

    FishBunCreator setSelectCircleStrokeColor(int strokeColor);

    FishBunCreator isStartInAllView(boolean isStartInAllView);
}
