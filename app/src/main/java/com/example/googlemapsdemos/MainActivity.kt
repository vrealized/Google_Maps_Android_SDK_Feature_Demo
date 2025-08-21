package com.example.googlemapsdemos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.googlemapsdemos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // 【核心修正】将DemoDetails的定义放在了正确的位置，即MainActivity类的内部。
    private data class DemoDetails(
        val titleResId: Int,
        val descriptionResId: Int,
        val activityClass: Class<out AppCompatActivity>
    )

    private val demos = listOf(
        DemoDetails(
            R.string.routes_demo_label,
            R.string.routes_demo_description,
            RoutesDemoActivity::class.java
        ),
        DemoDetails(
            R.string.geocoding_demo_label,
            R.string.geocoding_demo_description,
            GeocodingDemoActivity::class.java
        ),
        DemoDetails(
            R.string.heatmap_demo_label,
            R.string.heatmap_demo_description,
            HeatmapDemoActivity::class.java
        ),
        DemoDetails(
            R.string.advanced_markers_demo_label,
            R.string.advanced_markers_demo_description,
            AdvancedMarkersDemoActivity::class.java
        ),
        DemoDetails(
            R.string.fact_check_demo_label,
            R.string.fact_check_demo_description,
            FactCheckDemoActivity::class.java
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用ViewBinding来设置布局
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = DemoListAdapter(demos)
        }
    }

    // RecyclerView的适配器保持不变，但作为内部类以访问外部类的DemoDetails
    private inner class DemoListAdapter(private val demos: List<DemoDetails>) :
        RecyclerView.Adapter<DemoListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.text_view_title)
            val description: TextView = view.findViewById(R.id.text_view_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.demo_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val demo = demos[position]
            holder.title.setText(demo.titleResId)
            holder.description.setText(demo.descriptionResId)
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                context.startActivity(Intent(context, demo.activityClass))
            }
        }

        override fun getItemCount() = demos.size
    }
}