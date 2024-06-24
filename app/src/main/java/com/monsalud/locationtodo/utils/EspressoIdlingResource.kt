package com.monsalud.locationtodo.utils

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun incremenet() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }

    inline fun <T> wrapEspressoIdlingResource(function: () -> T) : T {
        EspressoIdlingResource.incremenet()
        return try {
            function()
        } finally {
            EspressoIdlingResource.decrement()
        }
    }
}