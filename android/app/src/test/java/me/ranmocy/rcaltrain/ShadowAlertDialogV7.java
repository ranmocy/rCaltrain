package me.ranmocy.rcaltrain;

import android.support.v7.app.AlertDialog;
import android.widget.Adapter;

import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.util.ReflectionHelpers;

@Implements(value = AlertDialog.class)
public class ShadowAlertDialogV7 extends ShadowDialog {

    private static ShadowAlertDialogV7 latest = null;

    public static ShadowAlertDialogV7 getLatestShadowAlertDialog() {
        return latest;
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

    public Adapter getAdapter() {
        Object controller = ReflectionHelpers.getField(realAlertDialog, "mAlert");
        return ReflectionHelpers.getField(controller, "mAdapter");
    }

    public ShadowListView getShadowListView() {
        return Shadows.shadowOf(realAlertDialog.getListView());
    }
}
