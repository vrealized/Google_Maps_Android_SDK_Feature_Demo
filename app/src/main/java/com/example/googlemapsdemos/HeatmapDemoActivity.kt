package com.example.googlemapsdemos // 确认包名

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.googlemapsdemos.databinding.HeatmapDemoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException

class HeatmapDemoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: HeatmapDemoBinding
    private var heatmapTileOverlay: TileOverlay? = null

    private val mapTypes = listOf(
        "标准 (Normal)" to GoogleMap.MAP_TYPE_NORMAL,
        "混合 (Hybrid)" to GoogleMap.MAP_TYPE_HYBRID,
        "卫星 (Satellite)" to GoogleMap.MAP_TYPE_SATELLITE,
        "地形 (Terrain)" to GoogleMap.MAP_TYPE_TERRAIN
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HeatmapDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupClickListeners()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 10f))
        addHeatMap()
    }

    private fun setupClickListeners() {
        binding.fabStreetView.setOnClickListener {
            if (!::map.isInitialized) return@setOnClickListener
            // 【激活】创建并启动StreetViewActivity
            val intent = Intent(this, StreetViewActivity::class.java).apply {
                putExtra(StreetViewActivity.EXTRA_LATLNG, map.cameraPosition.target)
            }
            startActivity(intent)
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
                heatmapTileOverlay?.remove()
                map.mapType = mapTypes[which].second
                addHeatMap()
            }
            .show()
    }

    private fun addHeatMap() {
        heatmapTileOverlay?.remove()
        lifecycleScope.launch {
            try {
                val tileOverlayOptions = withContext(Dispatchers.Default) {
                    val latLngList = readItems(R.raw.police_stations)
                    val provider = HeatmapTileProvider.Builder()
                        .data(latLngList)
                        .radius(50)
                        .build()
                    TileOverlayOptions().tileProvider(provider)
                }
                heatmapTileOverlay = map.addTileOverlay(tileOverlayOptions)
            } catch (_: Exception) {
                Toast.makeText(this@HeatmapDemoActivity, "创建热力图时出错", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Throws(JSONException::class)
    private fun readItems(resource: Int): List<LatLng> {
        val result = mutableListOf<LatLng>()
        val inputStream = resources.openRawResource(resource)
        val json = inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(LatLng(obj.getDouble("lat"), obj.getDouble("lng")))
        }
        return result
    }
}