package com.example.googlemapsdemos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.model.LatLng

class StreetViewActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback {

    private var position: LatLng? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.street_view_activity)

        position = intent.getParcelableExtraCompat(EXTRA_LATLNG, LatLng::class.java)

        if (position == null) {
            showErrorAndFinish(getString(R.string.error_getting_location))
            return
        }

        val streetViewPanoramaFragment =
            supportFragmentManager.findFragmentById(R.id.street_view_panorama_fragment) as? SupportStreetViewPanoramaFragment
        streetViewPanoramaFragment?.getStreetViewPanoramaAsync(this)
    }

    @SuppressLint("ConstantConditions")
    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama) {
        // 【核心修正】我们将所有与设置位置相关的逻辑都包裹在一个try-catch块中
        try {
            // 1. 先设置监听器，准备应对API的回调
            panorama.setOnStreetViewPanoramaChangeListener { streetViewPanoramaLocation ->
                // 这个回调会在setPosition之后被触发。
                // 如果API最终确认没有街景，links会为null。
                if (streetViewPanoramaLocation.links.isEmpty()) {                    // 这是处理“有坐标但无图像”的标准失败路径
                    showErrorAndFinish(getString(R.string.no_street_view_data))
                }
                // 如果streetViewPanoramaLocation不为null且links也不为null，说明成功了，我们什么都不用做。
            }

            // 2. 然后尝试设置位置
            position?.let {
                // 这个调用本身可能会因为网络问题或无效位置（如大海）而立即抛出异常
                panorama.setPosition(it, 200)
            } ?: run {
                // 如果position本身是null（虽然我们在onCreate已经检查过，但这是防御性编程）
                showErrorAndFinish(getString(R.string.error_getting_location))
            }
        } catch (e: Exception) {
            // 3. 捕获所有可能的异常，这是处理“立即失败”的路径
            Log.e(TAG, "设置街景位置时发生异常", e)
            showErrorAndFinish(getString(R.string.error_loading_street_view))
        }
    }

    /**
     * 统一的错误处理函数，增加了状态检查，确保只执行一次退出流程。
     */
    private fun showErrorAndFinish(message: String) {
        // 如果Activity已经在关闭过程中，就不要再执行任何操作
        if (isFinishing || isDestroyed) {
            return
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // 使用Handler确保Toast能显示出来，然后再关闭Activity
        handler.postDelayed({
            finish()
        }, 2000L) // 2秒延迟
    }

    override fun onDestroy() {
        super.onDestroy()
        // 在Activity销毁时，清除所有待处理的Handler消息，防止内存泄漏
        handler.removeCallbacksAndMessages(null)
    }

    private fun <T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    companion object {
        private val TAG = StreetViewActivity::class.java.simpleName
        @SuppressLint("SpellCheckingInspection")
        const val EXTRA_LATLNG = "extra_latlng"
    }
}