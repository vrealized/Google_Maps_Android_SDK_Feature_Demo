package com.example.googlemapsdemos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
//import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
//import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.googlemapsdemos.databinding.RoutesDemoBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RoutesDemoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: RoutesDemoBinding
    private lateinit var map: GoogleMap
    private var polylines: MutableList<Polyline> = mutableListOf()
    private var infoWindowMarker: Marker? = null

    private var originPlace: Place? = null
    private var currentUserLocation: LatLng? = null
    private var isOriginMyCurrentLocation = false
    private var destinationPlace: Place? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) enableMyLocation() }

    private val httpClient = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RoutesDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        setupAutocompleteFragments()
        setupClickListeners()
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap
        map.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        map.uiSettings.isZoomControlsEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(38.8951, -77.0364), 11f))
        enableMyLocation()
    }

    private fun setupAutocompleteFragments() {
        val originFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment_origin) as AutocompleteSupportFragment
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        originFragment.setPlaceFields(placeFields)
        originFragment.setHint(getString(R.string.origin_input_hint))
        originFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                originPlace = place
                isOriginMyCurrentLocation = false
            }
            override fun onError(status: com.google.android.gms.common.api.Status) {
                val errorMessage = status.statusMessage ?: "未知错误"
                Toast.makeText(this@RoutesDemoActivity, "地址选择出错: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        })

        val destinationFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment_destination) as AutocompleteSupportFragment
        destinationFragment.setPlaceFields(placeFields)
        destinationFragment.setHint(getString(R.string.destination_input_hint))
        destinationFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) { destinationPlace = place }
            override fun onError(status: com.google.android.gms.common.api.Status) {
                val errorMessage = status.statusMessage ?: "未知错误"
                Toast.makeText(this@RoutesDemoActivity, "地址选择出错: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        binding.buttonGetDirections.setOnClickListener { getDirections() }
        binding.fabMyLocation.setOnClickListener { moveToCurrentUserLocation() }
        binding.buttonPreview.setOnClickListener { previewRouteInGoogleMaps() }
        binding.buttonOriginMyLocation.setOnClickListener { setOriginToMyCurrentLocation() }
    }

    private fun getDirections() {
        val originWaypoint = if (isOriginMyCurrentLocation) {
            currentUserLocation?.let { Waypoint(location = LocationWrapper(LatLngData(it.latitude, it.longitude))) }
        } else {
            originPlace?.id?.let { Waypoint(placeId = it) }
        }
        if (originWaypoint == null || destinationPlace == null) {
            Toast.makeText(this, "请选择有效的出发地和目的地", Toast.LENGTH_SHORT).show(); return
        }
        binding.resultsBar.visibility = View.GONE
        infoWindowMarker?.remove()
        map.setPadding(0, 0, 0, 0)
        lifecycleScope.launch {
            try {
                val responseJson = withContext(Dispatchers.IO) {
                    val travelMode = when (binding.chipGroupTravelMode.checkedChipId) {
                        R.id.mode_walking -> "WALK"; R.id.mode_bicycle -> "BICYCLE"; R.id.mode_transit -> "TRANSIT"; else -> "DRIVE"
                    }
                    val requestBody = if (travelMode == "DRIVE") {
                        RouteRequest(origin = originWaypoint, destination = Waypoint(placeId = destinationPlace!!.id!!), travelMode = travelMode, routingPreference = "TRAFFIC_AWARE_OPTIMAL", computeAlternativeRoutes = true)
                    } else {
                        RouteRequest(origin = originWaypoint, destination = Waypoint(placeId = destinationPlace!!.id!!), travelMode = travelMode, routingPreference = null, computeAlternativeRoutes = true)
                    }
                    val requestBodyJson = gson.toJson(requestBody)
                    val request = Request.Builder()
                        .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                        .addHeader("X-Goog-FieldMask", "*")
                        .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                        .build()
                    httpClient.newCall(request).execute().body?.string()
                }
                if (responseJson != null) {
                    val routeResponse = gson.fromJson(responseJson, RouteResponse::class.java)
                    drawAllRoutes(routeResponse)
                } else {
                    Toast.makeText(this@RoutesDemoActivity, "未能获取路线数据", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoutesDemoActivity, "错误: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // 【核心修正】在函数上添加注解，一次性抑制所有拼写检查
    @SuppressLint("SpellCheckingInspection")
    private fun previewRouteInGoogleMaps() {
        val originParam = if (isOriginMyCurrentLocation) {
            currentUserLocation?.let { "${it.latitude},${it.longitude}" }
        } else {
            originPlace?.address
        }
        if (originParam == null || destinationPlace == null) {
            Toast.makeText(this, "请先规划路线", Toast.LENGTH_SHORT).show(); return
        }
        val travelModeChar = when (binding.chipGroupTravelMode.checkedChipId) {
            R.id.mode_walking -> "w"; R.id.mode_bicycle -> "b"; R.id.mode_transit -> "r"; else -> "d"
        }
        val uriString = "http://maps.google.com/maps?saddr=$originParam&daddr=${destinationPlace?.address}&dirflg=$travelModeChar"
        val uri = uriString.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "未找到谷歌地图应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawAllRoutes(response: RouteResponse) {
        polylines.forEach { it.remove() }; polylines.clear()
        infoWindowMarker?.remove()
        map.clear()
        if (response.routes.isNullOrEmpty()) { Toast.makeText(this, "未找到路线", Toast.LENGTH_SHORT).show(); return }

        response.routes.forEachIndexed { index, route ->
            val color = if (index == 0) Color.BLUE else Color.GRAY
            val path = com.google.maps.android.PolyUtil.decode(route.polyline.encodedPolyline)
            polylines.add(map.addPolyline(PolylineOptions().addAll(path).color(color).width(if (index == 0) 12f else 8f)))
        }

        val primaryRoute = response.routes[0]
        val durationInMinutes = (primaryRoute.duration.removeSuffix("s").toLongOrNull() ?: 0) / 60
        val distanceInKm = primaryRoute.distanceMeters / 1000.0
        val formattedDistance = "%.3f".format(distanceInKm)

        @SuppressLint("SpellCheckingInspection")
        val resultsTextForInfoWindow = "Distance: ${formattedDistance}KM\nDuration: ${durationInMinutes}min"
        val resultsTextForBottomBar = getString(R.string.route_results_format, durationInMinutes, distanceInKm)

        val path = com.google.maps.android.PolyUtil.decode(primaryRoute.polyline.encodedPolyline)
        if (path.isNotEmpty()) {
            showRouteInfoWindow(resultsTextForInfoWindow, path[path.size / 2])
        }
        showBottomResultsBar(resultsTextForBottomBar)

        val originMarkerPosition = if(isOriginMyCurrentLocation) currentUserLocation else originPlace?.latLng
        val originMarkerTitle = if(isOriginMyCurrentLocation) "我的位置" else "出发: ${originPlace?.name}"
        originMarkerPosition?.let { map.addMarker(MarkerOptions().position(it).title(originMarkerTitle).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))) }
        destinationPlace?.latLng?.let { map.addMarker(MarkerOptions().position(it).title("目的: ${destinationPlace?.name}")) }

        val bounds = LatLngBounds(LatLng(primaryRoute.viewport.low.latitude, primaryRoute.viewport.low.longitude), LatLng(primaryRoute.viewport.high.latitude, primaryRoute.viewport.high.longitude))
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    private fun showRouteInfoWindow(snippet: String, position: LatLng) {
        val markerOptions = MarkerOptions()
            .position(position)
            .title("DEFAULT_ROUTE")
            .snippet(snippet)
            .anchor(0.5f, 0.5f)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).alpha(0.01f)
        infoWindowMarker = map.addMarker(markerOptions)
        infoWindowMarker?.showInfoWindow()
    }

    private fun showBottomResultsBar(resultsText: String) {
        binding.textViewResults.text = resultsText
        binding.resultsBar.visibility = View.VISIBLE
        binding.resultsBar.post {
            val bottomPadding = binding.resultsBar.height
            map.setPadding(0, 0, 0, bottomPadding)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { enableMyLocation(); return }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentUserLocation = LatLng(it.latitude, it.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation!!, 15f))
            } ?: Toast.makeText(this, "无法获取当前位置，请确保GPS已开启", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setOriginToMyCurrentLocation() {
        if (currentUserLocation == null) {
            Toast.makeText(this, "正在获取您的位置，请稍后重试", Toast.LENGTH_SHORT).show()
            moveToCurrentUserLocation(); return
        }
        isOriginMyCurrentLocation = true
        originPlace = null
        val originFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment_origin) as AutocompleteSupportFragment
        originFragment.setText("")
        originFragment.setHint("已选择：我的当前位置")
        Toast.makeText(this, getString(R.string.origin_set_to_my_location), Toast.LENGTH_SHORT).show()
    }

    private class CustomInfoWindowAdapter(context: Context) : GoogleMap.InfoWindowAdapter {
        private val binding: com.example.googlemapsdemos.databinding.CustomInfoWindowBinding =
            com.example.googlemapsdemos.databinding.CustomInfoWindowBinding.inflate(LayoutInflater.from(context))
        override fun getInfoWindow(marker: Marker): View {
            binding.textTitle.text = marker.title
            binding.textSnippet.text = marker.snippet
            return binding.root
        }
        override fun getInfoContents(marker: Marker): View? = null
    }
}

// Data Classes
data class RouteRequest(val origin: Waypoint, val destination: Waypoint, val travelMode: String, val routingPreference: String? = null, val computeAlternativeRoutes: Boolean = false)
data class Waypoint(val address: String? = null, val placeId: String? = null, val location: LocationWrapper? = null)
data class LocationWrapper(val latLng: LatLngData)
data class RouteResponse(val routes: List<Route>?)
data class Route(val distanceMeters: Int, val duration: String, val polyline: PolylineData, val viewport: Viewport, val legs: List<RouteLeg>?)
data class PolylineData(val encodedPolyline: String)
data class Viewport(val low: LatLngData, val high: LatLngData)
data class LatLngData(val latitude: Double, val longitude: Double)
data class RouteLeg(val travelAdvisory: TravelAdvisory?)
data class TravelAdvisory(val speedReadingIntervals: List<SpeedReadingInterval>)
data class SpeedReadingInterval(val startPolylinePointIndex: Int, val endPolylinePointIndex: Int, val speed: String)