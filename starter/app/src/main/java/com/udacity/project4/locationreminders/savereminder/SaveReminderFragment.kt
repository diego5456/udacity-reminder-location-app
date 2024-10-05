package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.backgroundPermissions
import com.udacity.project4.utils.checkPermission
import com.udacity.project4.utils.locationPermissions
import com.udacity.project4.utils.permissionRequests
import com.udacity.project4.utils.registerBackgroundPermissionLauncher
import com.udacity.project4.utils.registerLocationPermissionLauncher
import com.udacity.project4.utils.runningQOrLater
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    private lateinit var geoFenceClient: GeofencingClient
    private val geoFencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var reminder: ReminderDataItem
    private lateinit var binding: FragmentSaveReminderBinding
    val locationPermissionLauncher = registerLocationPermissionLauncher(this)
    val backgroundPermissionLauncher =
        registerBackgroundPermissionLauncher(this, locationPermissionLauncher)

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
        permissionRequests(
            this, locationPermissionLauncher, backgroundPermissions
        )
        if (runningQOrLater) {
            permissionRequests(
                this, backgroundPermissionLauncher, backgroundPermissions
            )
        }
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
                addGeoFenceToReminder()
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
        if (checkPermission(this, locationPermissions) && checkPermission(
                this, backgroundPermissions
            )
        ) {
            return true
        } else {
            return false
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFenceToReminder() {
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

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}