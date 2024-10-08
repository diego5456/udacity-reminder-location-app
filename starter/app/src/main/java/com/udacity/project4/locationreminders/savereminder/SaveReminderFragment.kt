package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.TAG
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.backgroundPermissions
import com.udacity.project4.utils.checkPermission
import com.udacity.project4.utils.locationPermissions
import com.udacity.project4.utils.notificationsPermissions
import com.udacity.project4.utils.permissionRequests
import com.udacity.project4.utils.registerBackgroundPermissionLauncher
import com.udacity.project4.utils.registerLocationPermissionLauncher
import com.udacity.project4.utils.registerNotificationsPermissionLauncher
import com.udacity.project4.utils.runningQOrLater
import com.udacity.project4.utils.runningTiramisuOrLater
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.wrapEspressoIdlingResource
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    private lateinit var geoFenceClient: GeofencingClient
    private val geoFencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private val startActivityResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocation(false)
        }
    }

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var reminder: ReminderDataItem
    private lateinit var binding: FragmentSaveReminderBinding
    val locationPermissionLauncher = registerLocationPermissionLauncher(this) { checkPermissions() }
    val backgroundPermissionLauncher =
        registerBackgroundPermissionLauncher(this, locationPermissionLauncher)
    val notificationPermissionLauncher = registerNotificationsPermissionLauncher(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        geoFenceClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions =
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }
        binding.saveReminder.setOnClickListener {
            reminder = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )

            if (checkPermissions() && _viewModel.validateEnteredData(reminder)) {
                checkDeviceLocation()
            }
        }
    }

    fun checkPermissions(): Boolean {
        if (!checkPermission(this, locationPermissions)) {
            permissionRequests(this, locationPermissionLauncher, locationPermissions)
        }
        if (!checkPermission(this, backgroundPermissions) && runningQOrLater) {
            permissionRequests(this, backgroundPermissionLauncher, backgroundPermissions)
        }
        if (!checkPermission(this, notificationsPermissions) && runningTiramisuOrLater) {
            permissionRequests(this, locationPermissionLauncher, notificationsPermissions)
        }
        if (checkPermission(this, locationPermissions) && checkPermission(
                this, backgroundPermissions
            )
        ) {
            return true
        } else {
            return false
        }
    }


    fun checkDeviceLocation(
        resolve: Boolean = true
    ) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_LOW_POWER,
            1000
        ).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder)

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    startActivityResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocation()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnSuccessListener {
            // Call the passed function to start geofencing
            addGeoFenceToReminder()
        }
    }


    @SuppressLint("MissingPermission")
    private fun addGeoFenceToReminder() {
        wrapEspressoIdlingResource {
            if (!checkPermissions()) {
                return
            }
            val geoFence = Geofence.Builder().setRequestId(reminder.id).setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            ).setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

            val geofenceRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geoFence)
                .build()

            geoFenceClient.addGeofences(geofenceRequest, geoFencePendingIntent)?.run {
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(reminder)
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29