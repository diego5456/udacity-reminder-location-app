package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */


class AuthenticationActivity : AppCompatActivity() {

    private val viewModel by viewModels<LoginViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = DataBindingUtil.setContentView<ActivityAuthenticationBinding>(
            this,
            R.layout.activity_authentication
        )

        viewModel.authenticationState.observe(this, Observer {
            when (it) {
                AuthenticationState.AUTHENTICATED -> {
                    startActivity(Intent(this, RemindersActivity::class.java))
                    finish()
                }

                else -> {
                    binding.loginBtn.setOnClickListener {launchSignInFlow()}
                }
            }

        })

        // TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        authResult.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        )

    }

    private
    val authResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> Toast.makeText(
                    this,
                    "Success",
                    Toast.LENGTH_SHORT
                ).show()

                else -> Toast.makeText(
                    this,
                    "Failed to authenticate",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


}
