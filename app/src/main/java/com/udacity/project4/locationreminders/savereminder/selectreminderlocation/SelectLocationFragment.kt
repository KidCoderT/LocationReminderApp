package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
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
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.layout_bottom_sheet.*
import org.koin.android.ext.android.inject

// TODO: Hide the reminder text on select a location
// TODO: Add the add location fab button that returns user to the save reminder page
// TODO: Rework app so as to set the viewModel data only after the user selects the fab button
//  does the data get set onto the viewModel

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    //Use Koin to get the view model of the SaveReminder
    private lateinit var map: GoogleMap
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // the map setup implementation
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        // TODO: When the user confirms on the selected location,
        //  send back the selected location details to the view model
        //  and navigate back to the previous fragment to save the reminder and add the geofence
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        map.isMyLocationEnabled = true

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

        // added style to the map
        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )

        // put a marker to location that the user selected
        map.setOnPoiClickListener { poi ->

            // Update the map location Text to not have unnecessarily large blank text
            val poiName = poi.name.replace(
                "\n" +
                        "           \n" +
                        "             …", ""
            )

            // Set the new Data
            _viewModel.selectedPOI.value = poi
            _viewModel.latitude.value = poi.latLng.latitude
            _viewModel.longitude.value = poi.latLng.longitude
            _viewModel.reminderSelectedLocationStr.value = poiName

            // Move camera to the selected location
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    poi.latLng, 14F
                )
            )

            // Method for extra stuff
            updateMap()

            // show Bottom Drawer
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            location_title_text.text = poiName
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
                _viewModel.geofenceRadius.value = ((progress / 100.00) * maxCircleRadius)
                slider_amount.text = "${_viewModel.geofenceRadius.value!!.toInt()}m"
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

        slider_amount.text = "100m"


        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Reminding the user to select a location
        val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.duration = 2000L
        animator.addUpdateListener { animation ->
            binding.reminderText.alpha = animation.animatedValue as Float
        }
        animator.start()
    }

    private fun updateMap() {
        map.clear()

        val poiMarker = map.addMarker(
            MarkerOptions()
                .position(_viewModel.selectedPOI.value!!.latLng)
                .title(_viewModel.selectedPOI.value?.name)
        )

        // Add Circle based on radius
        map.addCircle(
            CircleOptions()
                .center(_viewModel.selectedPOI.value?.latLng)
                .radius(_viewModel.geofenceRadius.value!!)
                .strokeColor(Color.argb(255, 0, 0, 255))
                .fillColor(Color.argb(64, 0, 0, 255)).strokeWidth(2F)
        )

        poiMarker.showInfoWindow()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected.
        _viewModel.transitionType.value = parent.getItemAtPosition(pos).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}
}

private const val REQUEST_LOCATION_PERMISSION = 1
private const val maxCircleRadius = 2000.00
