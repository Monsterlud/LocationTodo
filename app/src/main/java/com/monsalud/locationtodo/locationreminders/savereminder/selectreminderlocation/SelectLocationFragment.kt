package com.monsalud.locationtodo.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.base.BaseFragment
import com.monsalud.locationtodo.databinding.FragmentSelectLocationBinding
import com.monsalud.locationtodo.locationreminders.RemindersActivity
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceConstants.REQUEST_LOCATION_PERMISSION
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceUtils
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderViewModel
import com.monsalud.locationtodo.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by inject()
    private val geofenceUtils by inject<GeofenceUtils>()

    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var selectLocationDialog: AlertDialog.Builder

    private lateinit var latLong: LatLng
    private lateinit var locationString: String
    private var isLocationSelected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Google Map setup implementation
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.locationChooserMap) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        binding.viewModel = _viewModel
        binding.btnSaveLocation.setOnClickListener {

            onLocationSelected()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = this

        selectLocationDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add a Location")
            .setMessage("Drop a pin on the map to add a location to associate with this reminder")
            .setIcon(R.drawable.ic_location)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        selectLocationDialog.show()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun onLocationSelected() {
        if (isLocationSelected) {
            _viewModel.reminderSelectedLocationStr.value = locationString
            _viewModel.latitude.value = latLong.latitude
            _viewModel.longitude.value = latLong.longitude
            findNavController().navigateUp()
        } else {
            Snackbar.make(
                binding.root,
                "Please select a location by long-pressing on the map",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
            }

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
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isMyLocationButtonEnabled = true
        setMapLongClick()
        setPoiClick(googleMap)
        enableMyLocation()
        moveCameraToCurrentLocation()
    }

    fun onLocationPermissionGranted() {
        if (::map.isInitialized && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            enableMyLocation()
            moveCameraToCurrentLocation()
        }
    }

    private fun setMapLongClick() {
        map.setOnMapLongClickListener { latlng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latlng.latitude,
                latlng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latlng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )

            isLocationSelected = true
            latLong = latlng
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                locationString = address.getAddressLine(0)
            }
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
        }
    }


    private fun moveCameraToCurrentLocation() {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            geofenceUtils.requestForegroundAndBackgroundLocationPermissions(
                requireContext(),
                requireActivity(),
                _viewModel.runningQOrLater.value!!
            )
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val zoomLevel = 15f
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel))
                    map.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Current Location Not Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            geofenceUtils.requestForegroundAndBackgroundLocationPermissions(
                requireContext(),
                requireActivity(),
                _viewModel.runningQOrLater.value!!
            )
        } else {
            map.isMyLocationEnabled = true
        }
    }
}
