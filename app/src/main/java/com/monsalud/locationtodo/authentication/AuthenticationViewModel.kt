package com.monsalud.locationtodo.authentication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.ktx.Firebase

private const val TAG = "AuthenticationViewModel"
class AuthenticationViewModel : ViewModel() {
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            Log.i(TAG, "*** User is signed in ***")
            AuthenticationState.AUTHENTICATED
        } else {
            Log.i(TAG, "*** User is signed out ***")
            AuthenticationState.UNAUTHENTICATED
        }
    }

}