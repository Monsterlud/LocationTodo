package com.monsalud.locationtodo.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.base.BaseFragment
import com.monsalud.locationtodo.databinding.FragmentSelectLocationBinding
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderViewModel
import com.monsalud.locationtodo.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val REQUEST_LOCATION_PERMISSION = 1

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
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

        // Google Map setup implementation
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.locationChooserMap) as SupportMapFragment
        supportMapFragment.getMapAsync(this)


        // TODO: add style to the map


        // TODO: call this function after the user confirms on the selected location
//            onLocationSelected()
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
        map.uiSettings.isMyLocationButtonEnabled = true

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
            moveCameraToCurrentLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun moveCameraToCurrentLocation() {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val zoomLevel = 15f
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel))
                map.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
            }
        }
    }

    @SuppressLint("MissingPermission") // Suppress warning since permission is checked
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }
}
