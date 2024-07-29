package com.udacity.project4.locationreminders

import android.os.IBinder
import android.view.WindowManager
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class ToastIdlingResource : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    @Volatile private var isIdle = true

    override fun getName(): String = "Toast IdlingResource"

    override fun isIdleNow(): Boolean = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.resourceCallback = callback
    }

    fun setIdleState(isIdleNow: Boolean) {
        this.isIdle = isIdleNow
        if (isIdleNow) {
            resourceCallback?.onTransitionToIdle()
        }
    }
}

class ToastMatcher : TypeSafeMatcher<Root?>() {

    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }

    public override fun matchesSafely(root: Root?): Boolean {
        val type: Int? = root?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken: IBinder = root.decorView.windowToken
            val appToken: IBinder = root.decorView.applicationWindowToken
            if (windowToken === appToken) {
                return true
            }
        }
        return false
    }
}