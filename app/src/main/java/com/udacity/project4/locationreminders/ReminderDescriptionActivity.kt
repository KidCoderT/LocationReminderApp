package com.udacity.project4.locationreminders

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment.Companion.MAX_CIRCLE_RADIUS


/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var reminderData: ReminderDataItem

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        reminderData = intent.extras?.getSerializable(EXTRA_ReminderDataItem) as ReminderDataItem

        // the implementation of the reminder details
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Updating the reminder dataItem data
        if (reminderData.description.isNullOrEmpty()) {
            reminderData.description = "No description"
        }

        binding.geofenceRadiusSlider.progress = ((reminderData.geofenceRadius/MAX_CIRCLE_RADIUS) * 100.00).toInt()
        binding.sliderAmount.text = getString(R.string.styled_meters_text, reminderData.geofenceRadius.toInt())

        binding.reminderLocationLatlng.text = getString(R.string.lat_long_snippet, reminderData.latitude, reminderData.longitude)

        binding.reminderDataItem = reminderData
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.isMyLocationEnabled = true

        val position = LatLng(reminderData.latitude!!, reminderData.longitude!!)

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                position, 14F
            )
        )
        val poiMarker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(reminderData.location!!)
        )

        // Add Circle based on radius
        map.addCircle(
            CircleOptions()
                .center(position)
                .radius(reminderData.geofenceRadius.toDouble())
                .strokeColor(Color.argb(255, 0, 0, 255))
                .fillColor(Color.argb(64, 0, 0, 255)).strokeWidth(2F)
        )

        poiMarker.showInfoWindow()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
