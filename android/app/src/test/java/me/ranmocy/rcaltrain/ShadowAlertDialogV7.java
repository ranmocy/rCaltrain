package me.ranmocy.rcaltrain;

import android.support.v7.app.AlertDialog;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowDialog;

@Implements(value = AlertDialog.class)
public class ShadowAlertDialogV7 extends ShadowDialog {

    private static ShadowAlertDialogV7 latest = null;

    public static AlertDialog getLatestAlertDialog() {
        return latest.realAlertDialog;
    }

    @Resetter
    public static void reset() {
        latest = null;
    }

    @RealObject
    private AlertDialog realAlertDialog;

    @Implementation
    public void show() {
        super.show();
        latest = this;
    }
}
