package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.round
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import kotlinx.android.synthetic.main.layout_bottom_sheet.*
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback,
    AdapterView.OnItemSelectedListener {

    private lateinit var map: GoogleMap

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var mapFragment: SupportMapFragment
    private var permissionGranted = false
    private var selectedPOI: PointOfInterest? = null
    private var updatedPOIName: String? = null
    private var transitionType: String = "Enter"
    private var geofenceRadius: Float = 100.0F
    private var selectedMapLatLng: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        checkPermissionsAndSetupMap()

        return binding.root
    }

    private fun onLocationSelected() {
        // When the user confirms on the selected location,
        //  send back the selected location details to the view model
        //  and navigate back to the previous fragment to save the reminder and add the geofence

        // 1. Set data in viewModel
        _viewModel.selectedPOI.value = selectedPOI
        _viewModel.selectedMapLatLng.value = selectedMapLatLng
        if (selectedPOI != null) {
            _viewModel.latitude.value = selectedPOI?.latLng?.latitude
            _viewModel.longitude.value = selectedPOI?.latLng?.longitude
        } else {
            _viewModel.latitude.value = selectedMapLatLng?.latitude
            _viewModel.longitude.value = selectedMapLatLng?.longitude
        }
        _viewModel.reminderSelectedLocationStr.value = updatedPOIName
        _viewModel.transitionType.value = transitionType
        _viewModel.geofenceRadius.value = geofenceRadius

        // 2. return back to save reminder screen
        view?.findNavController()?.popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
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

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionsAndSetupMap() {
        if (isPermissionGranted()) {
            permissionGranted = true
            mapFragment =
                childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // added style to the map
        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )

        // put a marker to location that the user selected
        map.setOnPoiClickListener { poi ->

            // Set the new Data
            selectedPOI = poi
            updatedPOIName = poi.name.replace(
                "\n" +
                        "           \n" +
                        "             …", ""
            ) // Update the map location Text to not have unnecessarily large blank text

            // Move camera to the selected location
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    poi.latLng, 14F
                )
            )

            // Method for extra stuff
            updateMap()

            // hide the reminder text
            binding.reminderText.visibility = View.GONE

            // Show the select this location fab button
            select_location_fab.show()

            // show Bottom Drawer
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            location_title_text.text = updatedPOIName
        }

        // put a marker to location that the user selected
        map.setOnMapClickListener { pos ->

            // Set the new Data
            selectedPOI = null
            updatedPOIName = "(${pos.latitude.round(3)}, ${pos.longitude.round(3)})"
            selectedMapLatLng = pos

            // Move camera to the selected location
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pos, 14F
                )
            )

            // Method for extra stuff
            updateMap()

            // hide the reminder text
            binding.reminderText.visibility = View.GONE

            // Show the select this location fab button
            select_location_fab.show()

            // show Bottom Drawer
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            location_title_text.text = updatedPOIName
        }

        if (permissionGranted) {
            // Shows my location only if the permission is granted
            map.isMyLocationEnabled = permissionGranted

            // zoom to the user location after taking his permission
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location!!.latitude,
                                location.longitude
                            ), 14F
                        )
                    )
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom sheet
        val sheetOnExpandedClickListener = View.OnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val sheetOnCollapsedClickListener = View.OnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        bottomSheetBehavior =
            BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottom_sheet_title_container.setOnClickListener(sheetOnExpandedClickListener)
                } else {
                    bottom_sheet_title_container.setOnClickListener(sheetOnCollapsedClickListener)
                }
            }
        })

        geofence_radius_slider.isFocusedByDefault = true
        geofence_radius_slider.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seek: SeekBar,
                progress: Int, fromUser: Boolean
            ) {
                geofenceRadius = (((progress / 100.00) * MAX_CIRCLE_RADIUS).toFloat())
                slider_amount.text =
                    getString(R.string.styled_meters_text, geofenceRadius.toInt())
                updateMap()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                updateMap()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateMap()
            }
        })

        val spinner: Spinner = transitions_spinner
        val items = arrayOf("Enter", "Dwell", "Exit")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this

        // Reminding the user to select a location
        val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.duration = 2000L
        animator.addUpdateListener { animation ->
            binding.reminderText.alpha = animation.animatedValue as Float
        }

        // setup the fab button
        select_location_fab.setOnClickListener {
            onLocationSelected()
        }

        slider_amount.text = getString(R.string.styled_meters_text, 100)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        animator.start()
        select_location_fab.hide()
    }

    private fun updateMap() {
        map.clear()

        if (selectedPOI != null) {
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(selectedPOI!!.latLng)
                    .title(selectedPOI?.name)
            )

            // Add Circle based on radius
            map.addCircle(
                CircleOptions()
                    .center(selectedPOI?.latLng)
                    .radius(geofenceRadius.toDouble())
                    .strokeColor(Color.argb(255, 0, 0, 255))
                    .fillColor(Color.argb(64, 0, 0, 255)).strokeWidth(2F)
            )

            poiMarker.showInfoWindow()
        } else {
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(selectedMapLatLng!!)
                    .title(updatedPOIName)
            )

            // Add Circle based on radius
            map.addCircle(
                CircleOptions()
                    .center(selectedMapLatLng!!)
                    .radius(geofenceRadius.toDouble())
                    .strokeColor(Color.argb(255, 0, 0, 255))
                    .fillColor(Color.argb(64, 0, 0, 255)).strokeWidth(2F)
            )

            poiMarker.showInfoWindow()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionGranted = true
            } else {
                _viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
            }
            mapFragment =
                childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected.
        transitionType = parent.getItemAtPosition(pos).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    companion object {
        val MAX_CIRCLE_RADIUS = 2000.00
    }
}

private const val REQUEST_LOCATION_PERMISSION = 1
