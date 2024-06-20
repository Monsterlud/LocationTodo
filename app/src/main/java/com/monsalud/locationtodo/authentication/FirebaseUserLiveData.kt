package com.monsalud.locationtodo.authentication

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseUserLiveData : LiveData<FirebaseUser?>() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        value = firebaseAuth.currentUser
        Log.i(TAG, "*** authStateListener ***")
    }

    override fun onActive() {
        firebaseAuth.addAuthStateListener(authStateListener)
        Log.i(TAG, "*** onActive ***")
    }

    override fun onInactive() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        Log.i(TAG, "*** onInactive ***")
    }

    companion object {
        private const val TAG = "FirebaseUserLiveData"
    }
}