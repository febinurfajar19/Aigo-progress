package com.example.aigo.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.aigo.LocationViewModel
import com.example.aigo.R
import com.example.aigo.constant.AppConstant
import com.example.aigo.databinding.FragmentHomeBinding
import com.example.aigo.model.googlePlaceModel.GooglePlaceModel
import com.example.aigo.model.googlePlaceModel.GoogleResponseModel
import com.example.aigo.permissions.AppPermissions
import com.example.aigo.utility.LoadingDialog
import com.example.aigo.utility.State
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentHomeBinding
    private var mGoogleMap: GoogleMap? = null
    private lateinit var appPermission: AppPermissions
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var permissionToRequest = mutableListOf<String>()
    private var isLocationPermissionOk = false
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var currentLocation:Location
    private var currentMarker: Marker? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private var isTrafficEnable: Boolean = false
    private var radius = 1500
    private val locationViewModel: LocationViewModel by viewModels<LocationViewModel>()
    private lateinit var googlePlaceList: ArrayList<GooglePlaceModel>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appPermission = AppPermissions()
        loadingDialog = LoadingDialog(requireActivity())
        firebaseAuth = Firebase.auth
        googlePlaceList = ArrayList()

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isLocationPermissionOk =
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                            && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

                if (isLocationPermissionOk)
                    setUpGoogleMap()
                else
                    Snackbar.make(binding.root, "Location permission denied", Snackbar.LENGTH_LONG)
                        .show()

            }

        val mapFragment =
            (childFragmentManager.findFragmentById(R.id.homeMap) as SupportMapFragment?)
        mapFragment?.getMapAsync(this)

        for (placeModel in AppConstant.placesName) {
            val chip = Chip(requireContext())
            chip.text = placeModel.name
            chip.id = placeModel.id
            chip.setPadding(5, 5, 5, 5)
            chip.setTextColor(resources.getColor(R.color.primaryColor, null))
            chip.chipBackgroundColor = resources.getColorStateList(R.color.white, null)
            chip.chipIcon = ResourcesCompat.getDrawable(resources, placeModel.drawableId, null)
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            binding.placesGroup.addView(chip)
        }

        binding.enableTraffic.setOnClickListener {

            if (isTrafficEnable) {
                mGoogleMap?.apply {
                    isTrafficEnabled = false
                    isTrafficEnable = false
                }
            } else {
                mGoogleMap?.apply {
                    isTrafficEnabled = true
                    isTrafficEnable = true
                }
            }
        }

        binding.currentLocation.setOnClickListener { getCurrentLocation() }

        binding.btnMapType.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)

            popupMenu.apply {
                menuInflater.inflate(R.menu.map_type_menu, popupMenu.menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {

                        R.id.btnNormal -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        R.id.btnSatellite -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        R.id.btnTerrain -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    }
                    true
                }

                show()
            }
        }

        binding.placesGroup.setOnCheckedChangeListener { _, checkedId ->

            if (checkedId != -1) {
                val placeModel = AppConstant.placesName[checkedId - 1]
                binding.edtPlaceName.setText(placeModel.name)
                getNearByPlace(placeModel.placeType)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                isLocationPermissionOk = true
                setUpGoogleMap()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Location Permission")
                    .setMessage("Aigo required location permission to access your location")
                    .setPositiveButton("Ok") { _, _ ->
                        requestLocation()
                    }.create().show()
            }

            else -> {
                requestLocation()
            }
        }

    }

    private fun requestLocation() {
        permissionToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        permissionLauncher.launch(permissionToRequest.toTypedArray())
    }

    private fun setUpGoogleMap() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionOk = false
            return
        }
        mGoogleMap?.isMyLocationEnabled = true
        mGoogleMap?.uiSettings?.isTiltGesturesEnabled = true

        setUpLocationUpdate()
    }

    private fun setUpLocationUpdate() {
        locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                for (location in locationResult?.locations!!) {
                    Log.d("TAG", "onLocationResult: ${location.longitude} ${location.latitude}")
                }
            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionOk = false
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Location update start", Toast.LENGTH_SHORT).show()
            }
        }

        getCurrentLocation()
    }

    private fun getCurrentLocation(){
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionOk = false
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {

            currentLocation = it
            moveCameraToLocation(currentLocation)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "$it", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveCameraToLocation(location: Location) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            LatLng(
                -6.9730017,
                107.6294967
            ), 17f
        )

        val markerOption = MarkerOptions()
            .position(LatLng(-6.9730017, 107.6294967))
            .title("Current Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .snippet(firebaseAuth.currentUser?.displayName)

        currentMarker?.remove()
        currentMarker = mGoogleMap?.addMarker(markerOption)
        currentMarker?.tag = 703
        mGoogleMap?.animateCamera(cameraUpdate)

    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        Log.d("TAG", "stopLocationUpdates: Location Update Stop")
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if(fusedLocationProviderClient!=null){
            startLocationUpdates()
            currentMarker?.remove()
        }
    }


    private fun getNearByPlace(placeType: String) {
        val url = ("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + currentLocation.latitude + "," + currentLocation.longitude
                + "&radius=" + radius + "&type=" + placeType + "&key=" +
                resources.getString(R.string.API_KEY))

        lifecycleScope.launchWhenStarted {
            locationViewModel.getNearByPlace(url).collect {
                when (it) {
                    is State.Loading -> {
                        if (it.flag == true) {
                            loadingDialog.startLoading()
                        }
                    }
                    is State.Success -> {
                        loadingDialog.stopLoading()
                        val googleResponseModel: GoogleResponseModel =
                            it.data as GoogleResponseModel

                        if (googleResponseModel.googlePlaceModelList !== null &&
                            googleResponseModel.googlePlaceModelList.isNotEmpty()
                        ) {
                            googlePlaceList.clear()
                            mGoogleMap?.clear()

                            for (i in googleResponseModel.googlePlaceModelList.indices) {
                                googlePlaceList.add(googleResponseModel.googlePlaceModelList[i])
                                addMarker(googleResponseModel.googlePlaceModelList[i], i)
                            }

                        } else {
                            mGoogleMap?.clear()
                            googlePlaceList.clear()

                        }
                    }
                    is State.Failed -> {
                        loadingDialog.stopLoading()
                        Snackbar.make(
                            binding.root, it.error,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun addMarker(googlePlaceModel: GooglePlaceModel, position: Int) {
        val markerOptions = MarkerOptions()
            .position(
                LatLng(
                    googlePlaceModel.geometry?.location?.lat!!,
                    googlePlaceModel.geometry.location.lng!!
                )
            )
            .title(googlePlaceModel.name)
            .snippet(googlePlaceModel.vicinity)

        markerOptions.icon(getCustomIcon())
        mGoogleMap?.addMarker(markerOptions)?.tag = position
    }

    private fun getCustomIcon(): BitmapDescriptor {
        val background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
        background?.setTint(resources.getColor(R.color.quantum_googred900, null))
        background?.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            background?.intrinsicWidth!!, background.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}