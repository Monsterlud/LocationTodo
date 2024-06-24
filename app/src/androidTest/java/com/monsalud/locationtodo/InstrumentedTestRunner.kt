package com.monsalud.locationtodo

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.monsalud.locationtodo.locationreminders.TestApplication

class InstrumentedTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }
}
