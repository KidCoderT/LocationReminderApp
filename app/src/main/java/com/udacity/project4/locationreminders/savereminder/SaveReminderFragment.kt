package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.provider.Settings
import androidx.annotation.NonNull
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.GeofencingClient

import com.google.android.gms.maps.GoogleMap
import com.udacity.project4.BuildConfig


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
    private lateinit var newReminder: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val geofenceRadius = _viewModel.geofenceRadius.value
            val transitionType = if (_viewModel.transitionType.value != null) _viewModel.transitionType.value else "Enter"


            // use the user entered reminder details to:
            newReminder = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude,
                geofenceRadius!!, transitionType!!)

            // 1) validate the reminder
            if (_viewModel.validateEnteredData(newReminder)) {
                // 2) check permissions and create geofence request
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createNewGeofenceRequest() {
        // Create geofence
        val geofence = Geofence.Builder()
            .setRequestId(newReminder.id)
            .setCircularRegion(
                newReminder.latitude!!,
                newReminder.longitude!!,
                newReminder.geofenceRadius
            )
            .setTransitionTypes(when(newReminder.transitionType) {
                "Enter" -> Geofence.GEOFENCE_TRANSITION_ENTER
                "Dwell" -> Geofence.GEOFENCE_TRANSITION_DWELL
                else -> Geofence.GEOFENCE_TRANSITION_EXIT
            })
            .setLoiteringDelay(500)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        // Create pending intent
        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_EVENT
            intent.putExtra("transitionType", newReminder.transitionType)
            PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                _viewModel.showToast.postValue(getString(R.string.geofence_entered))
                _viewModel.validateAndSaveReminder(newReminder)
            }
            addOnFailureListener {
                _viewModel.showToast.postValue(getString(R.string.error_adding_geofence))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful && !isDetached ) {
                createNewGeofenceRequest()
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Snackbar.make(
                requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "RemindersActivity.project4.action.ACTION_GEOFENCE_EVENT"
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "SaveReminderFragement"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

