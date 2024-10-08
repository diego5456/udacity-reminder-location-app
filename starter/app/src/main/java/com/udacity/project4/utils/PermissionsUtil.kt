package com.udacity.project4.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R


lateinit var backgroundPermissionLauncher: ActivityResultLauncher<String>
val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
val runningTiramisuOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun registerRequestMultiplePermissionsLauncher(fragment: Fragment): ActivityResultLauncher<Array<String>> {
    return fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                when (it.key) {
                    Manifest.permission.POST_NOTIFICATIONS -> createPermissionDeniedSnackbar(
                        fragment,
                        notificationsPermissions.toastMessage
                    )


                    Manifest.permission.ACCESS_FINE_LOCATION -> createPermissionDeniedSnackbar(
                        fragment,
                        locationPermissions.toastMessage
                    )

                    Manifest.permission.ACCESS_BACKGROUND_LOCATION -> createPermissionDeniedSnackbar(
                        fragment,
                        locationPermissions.toastMessage
                    )
                }
            }
        }
    }
}

fun registerNotificationsPermissionLauncher(fragment: Fragment): ActivityResultLauncher<String> {
    return fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (!it) {
            createPermissionDeniedSnackbar(fragment, notificationsPermissions.toastMessage)
        }
    }
}

fun registerLocationPermissionLauncher(fragment: Fragment, resumeFunction : () -> Unit): ActivityResultLauncher<String> {
    return fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (!it) {
            createPermissionDeniedSnackbar(fragment, locationPermissions.toastMessage)
        } else {
            resumeFunction()
        }
    }
}

fun registerBackgroundPermissionLauncher(
    fragment: Fragment,
    locationPermissionLauncher: ActivityResultLauncher<String>
): ActivityResultLauncher<String> {
    return fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (!it) {
            if (ActivityCompat.checkSelfPermission(
                    fragment.requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionRequests(
                    fragment,
                    locationPermissionLauncher,
                    locationPermissions
                )
            }
            createPermissionDeniedSnackbar(
                fragment,
                backgroundPermissions.toastMessage
            )
        }
    }
}


fun createPermissionDeniedSnackbar(fragment: Fragment, content: String) {
    Snackbar.make(
        fragment.requireView(),
        content,
        Snackbar.LENGTH_LONG
    )
        .setAction(R.string.settings) {
            fragment.startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.show()
}


fun multiplePermissionsRequest(
    fragment: Fragment,
    requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>,
    permissions: Array<ReminderAppPermissions>
) {
    val listOfPermissions = mutableListOf<String>()
    permissions.apply {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(
                    fragment.requireContext(),
                    it.permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (fragment.shouldShowRequestPermissionRationale(it.permission)) {
                    createAlertDialog(
                        fragment,
                        it.rationaleTitle,
                        it.rationaleMessage,
                        { requestMultiplePermissionsLauncher.launch(arrayOf(it.permission)) })
                }
            }
            listOfPermissions.add(it.permission)
        }

    }
    if (listOfPermissions.isNotEmpty()) {
        requestMultiplePermissionsLauncher.launch(listOfPermissions.toTypedArray())
    }
}


fun permissionRequests(
    fragment: Fragment,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    permission: ReminderAppPermissions
) {
    val permissionsCheck = ContextCompat.checkSelfPermission(
        fragment.requireContext(),
        permission.permission
    )
    if (permissionsCheck != PackageManager.PERMISSION_GRANTED) {

        requestPermissionLauncher.launch(permission.permission)
        createAlertDialog(
            fragment,
            permission.rationaleTitle,
            permission.rationaleMessage
        ) { requestPermissionLauncher.launch(permission.permission) }

    } else {
        requestPermissionLauncher.launch(permission.permission)
    }
}

fun checkPermission(fragment: Fragment, permission: ReminderAppPermissions): Boolean {
    return ContextCompat.checkSelfPermission(
        fragment.requireContext(),
        permission.permission
    ) == PackageManager.PERMISSION_GRANTED
}


private fun createAlertDialog(
    fragment: Fragment,
    title: String,
    message: String,
    positiveAction: () -> Unit
) {
    androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            positiveAction()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}


val TAG = "PermissionsUtil"
val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
val REQUEST_TURN_DEVICE_LOCATION_ON = 29
val LOCATION_PERMISSION_INDEX = 0
val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

val backgroundPermissions = ReminderAppPermissions(
    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    "Background location permission",
    "You need to grant location permission turned to always in order to use this app.",
    "Please allow Location access all the time"

)
val notificationsPermissions = ReminderAppPermissions(
    Manifest.permission.POST_NOTIFICATIONS,
    "Notifications permission",
    "You need to grant notifications permission turned to in order to received reminders.",
    "Please allow notifications"

)
val locationPermissions = ReminderAppPermissions(
    Manifest.permission.ACCESS_FINE_LOCATION,
    "Location permission",
    "You need to grant location permission in order to select the location.",
    "Please allow Location access"
)