package com.example.googlemapsdemos

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.googlemapsdemos.databinding.GeocodingDemoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GeocodingDemoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var currentMarker: Marker? = null
    private lateinit var geocoder: Geocoder
    private lateinit var binding: GeocodingDemoBinding

    private var lastValidClickedLocation: LatLng? = null

    private val mapTypes = listOf(
        "标准 (Normal)" to GoogleMap.MAP_TYPE_NORMAL,
        "混合 (Hybrid)" to GoogleMap.MAP_TYPE_HYBRID,
        "卫星 (Satellite)" to GoogleMap.MAP_TYPE_SATELLITE,
        "地形 (Terrain)" to GoogleMap.MAP_TYPE_TERRAIN
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GeocodingDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        geocoder = Geocoder(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupClickListeners()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        map.setOnMapClickListener { latLng ->
            reverseGeocode(latLng)
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.7128, -74.0060), 10f))
    }

    private fun setupClickListeners() {
        binding.fabStreetView.setOnClickListener {
            lastValidClickedLocation?.let { validLatLng ->
                val intent = Intent(this, StreetViewActivity::class.java).apply {
                    putExtra(StreetViewActivity.EXTRA_LATLNG, validLatLng)
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "请先在地图上选择一个有效的陆地位置", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabLayers.setOnClickListener {
            showMapTypeSelectorDialog()
        }
    }

    private fun showMapTypeSelectorDialog() {
        if (!::map.isInitialized) return
        val mapTypeNames = mapTypes.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择地图图层")
            .setItems(mapTypeNames) { _, which ->
                map.mapType = mapTypes[which].second
            }
            .show()
    }

    private fun reverseGeocode(latLng: LatLng) {
        val progressToast = Toast.makeText(this, getString(R.string.geocoding_in_progress), Toast.LENGTH_SHORT)
        progressToast.show()

        lastValidClickedLocation = null

        lifecycleScope.launch {
            try {
                // 【核心修正】调用新的、高性能的suspend函数
                val address = getAddressFromLocationSuspend(latLng)
                progressToast.cancel()
                updateMapWithAddress(latLng, address)
            } catch (e: Exception) {
                // 统一处理所有异常
                progressToast.cancel()
                Toast.makeText(this@GeocodingDemoActivity, e.message ?: getString(R.string.geocoder_not_available), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    /**
     * 【核心重构】使用suspendCancellableCoroutine将回调API转换为现代的suspend函数，
     * 这是处理这类异步操作的最佳实践，可以完全避免阻塞和ANR。
     */
    private suspend fun getAddressFromLocationSuspend(latLng: LatLng): Address? {
        // 确保所有IO和计算都在后台线程
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 对于新版API，使用suspendCancellableCoroutine包装回调
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        if (continuation.isActive) { // 检查协程是否还在活动
                            continuation.resume(addresses.firstOrNull())
                        }
                    }
                }
            } else {
                // 对于旧版API，直接在IO线程中执行阻塞调用
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.firstOrNull()
                } catch (e: IOException) {
                    throw IOException("地理编码服务不可用或网络错误", e)
                }
            }
        }
    }

    private fun updateMapWithAddress(latLng: LatLng, address: Address?) {
        currentMarker?.remove()

        val title: String
        if (address != null) {
            lastValidClickedLocation = latLng
            title = address.getAddressLine(0)
        } else {
            title = getString(R.string.no_address_found)
        }

        val snippet = "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
        val markerOptions = MarkerOptions().position(latLng).title(title).snippet(snippet)
        currentMarker = map.addMarker(markerOptions)
        currentMarker?.showInfoWindow()
    }
}