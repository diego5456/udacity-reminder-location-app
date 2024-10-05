package com.udacity.project4.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

enum class AuthenticationState {
    AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
}

class LoginViewModel: ViewModel() {
    val authenticationState: LiveData<AuthenticationState> = FirebaseUserLiveData().map {
        if (it != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

}