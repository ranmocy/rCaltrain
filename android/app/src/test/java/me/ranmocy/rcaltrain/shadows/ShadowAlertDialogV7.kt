package me.ranmocy.rcaltrain.shadows

import android.support.v7.app.AlertDialog
import android.widget.Adapter

import org.robolectric.Shadows
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.annotation.Resetter
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowListView
import org.robolectric.util.ReflectionHelpers

@Implements(value = AlertDialog::class)
class ShadowAlertDialogV7 : ShadowDialog() {

    companion object {

        var latestShadowAlertDialog: ShadowAlertDialogV7? = null
            private set

        @Resetter
        fun reset() {
            latestShadowAlertDialog = null
        }
    }

    @RealObject
    private val realAlertDialog: AlertDialog? = null

    @Implementation
    override fun show() {
        super.show()
        latestShadowAlertDialog = this
    }

    val adapter: Adapter
        get() {
            val controller = ReflectionHelpers.getField<Any>(realAlertDialog!!, "mAlert")
            return ReflectionHelpers.getField<Adapter>(controller, "mAdapter")
        }

    val shadowListView: ShadowListView
        get() = Shadows.shadowOf(realAlertDialog!!.listView)
}
