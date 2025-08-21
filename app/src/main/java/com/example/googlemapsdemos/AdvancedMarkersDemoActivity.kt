package com.example.googlemapsdemos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.googlemapsdemos.databinding.AdvancedMarkersDemoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

class AdvancedMarkersDemoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var clusterManager: ClusterManager<MyItem>
    private lateinit var binding: AdvancedMarkersDemoBinding

    private val mapTypes = listOf(
        "标准 (Normal)" to GoogleMap.MAP_TYPE_NORMAL,
        "混合 (Hybrid)" to GoogleMap.MAP_TYPE_HYBRID,
        "卫星 (Satellite)" to GoogleMap.MAP_TYPE_SATELLITE,
        "地形 (Terrain)" to GoogleMap.MAP_TYPE_TERRAIN
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdvancedMarkersDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupClickListeners()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap
        map.apply {
            uiSettings.isZoomControlsEnabled = true
            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.4, -122.1), 10f))
        }

        setupClusterManager()
    }

    private fun setupClickListeners() {
        binding.fabStreetView.setOnClickListener {
            if (!::map.isInitialized) return@setOnClickListener
            val intent = Intent(this, StreetViewActivity::class.java).apply {
                putExtra(StreetViewActivity.EXTRA_LATLNG, map.cameraPosition.target)
            }
            startActivity(intent)
        }

        binding.fabLayers.setOnClickListener {
            showMapTypeSelectorDialog()
        }
    }

    /**
     * 【核心修正】添加注解，忽略关于监听器可能被覆盖的警告。
     * 因为我们正在使用聚合功能，所以这是必要且安全的操作。
     */
    @SuppressLint("PotentialBehaviorOverride")
    private fun setupClusterManager() {
        clusterManager = ClusterManager(this, map)
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)
        addClusterItemsInBackground()
    }

    private fun addClusterItemsInBackground() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.Default) {
                val random = Random()
                val itemList = mutableListOf<MyItem>()
                for (i in 0 until 100) {
                    val lat = 37.2 + (0.3 * random.nextDouble())
                    val lng = -122.4 + (0.6 * random.nextDouble())
                    itemList.add(MyItem(LatLng(lat, lng), "Marker #$i", "Item #$i"))
                }
                itemList
            }
            clusterManager.addItems(items)
            clusterManager.cluster()
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

    data class MyItem(
        private val position: LatLng,
        private val title: String,
        private val snippet: String,
    ) : ClusterItem {
        override fun getPosition(): LatLng = position
        override fun getTitle(): String = title
        override fun getSnippet(): String = snippet
    }
}