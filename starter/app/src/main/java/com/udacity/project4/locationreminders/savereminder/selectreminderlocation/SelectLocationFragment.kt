package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.utils.checkPermission
import com.udacity.project4.utils.locationPermissions
import com.udacity.project4.utils.permissionRequests
import com.udacity.project4.utils.registerLocationPermissionLauncher
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    val TAG = SelectLocationFragment::class.java.simpleName

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private val locationPermissionsLauncher = registerLocationPermissionLauncher(this){
        enableMyLocation()
    }
    private lateinit var binding: FragmentSelectLocationBinding
    val defaultLocation = LatLng(-34.0, 151.0)
    private var coordinates: LatLng? = null

    @VisibleForTesting
    var currentMarker: Marker? = null
    private var poi: PointOfInterest? = null

    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private val startActivityResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            enableMyLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        onLocationSelected()
        return binding.root
    }


    private fun enableMyLocation() {
        if (checkPermission(this, locationPermissions)) {
            map.isMyLocationEnabled = true
        } else {
            permissionRequests(
                this,
                locationPermissionsLauncher,
                locationPermissions
            )
        }
    }

    private fun checkDeviceLocation(
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
                    Log.d(com.udacity.project4.locationreminders.TAG, "Error getting location settings resolution: ${sendEx.message}")
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
            getDeviceLocation()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location ->
            if (location != null) {
                val currentLatLong = LatLng(location.latitude, location.longitude)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLong, 15f)
                map.animateCamera(cameraUpdate)
            } else {
                Log.d(TAG, "Location null")
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
            }

        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.selector_map) as SupportMapFragment
        mapFragment.getMapAsync(this) ?: Log.e("MapError", "Fragment not found!")

    }

    private fun onLocationSelected() {
        binding.saveBtn.setOnClickListener {
            if (currentMarker != null) {
                _viewModel.latitude.value = coordinates!!.latitude
                _viewModel.longitude.value = coordinates!!.longitude
                if (poi != null) {
                    _viewModel.selectedPOI.value = poi
                    _viewModel.reminderSelectedLocationStr.value = poi!!.name
                } else {
                    _viewModel.reminderSelectedLocationStr.value = "Custom Location"
                }
                _viewModel.navigationCommand.value = NavigationCommand.Back
            } else {
                Snackbar.make(
                    this.requireView(),
                    "Please select a location",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
        mapFragment = childFragmentManager.findFragmentById(R.id.selector_map) as SupportMapFragment
        mapFragment.getMapAsync(this) // Triggers onMapReady callback
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            currentMarker?.remove()
            coordinates = latLng
            currentMarker =
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                )
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            currentMarker?.remove()
            this.poi = poi
            coordinates = poi.latLng
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            currentMarker?.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            Log.d(TAG, "Style parsing success")
        } catch (e: Exception) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        setMapClick(map)
        enableMyLocation()
        checkDeviceLocation()
        setPoiClick(map)
        Log.d(TAG, "Map is ready")
    }

}