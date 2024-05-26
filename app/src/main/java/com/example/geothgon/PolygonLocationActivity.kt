package com.example.geothgon

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs

class PolygonLocationActivity : AppCompatActivity(), LocationListener {
    private var latitude = 0.0
    private var longitude = 0.0
    private val points = mutableListOf<Pair<Double, Double>>()
    private lateinit var locationManager: LocationManager
    private lateinit var pointsTextView: TextView
    private lateinit var pointNowTextView: TextView
    private lateinit var areaEvalTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_polygon_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        pointsTextView = findViewById(R.id.pointsTextView)
        pointNowTextView = findViewById(R.id.pointNowTextView)
        areaEvalTextView = findViewById(R.id.areaEvalTextView)

        findViewById<Button>(R.id.buttonPointing).setOnClickListener {
            val point = Pair(latitude, longitude)
            if (!points.contains(point)) {
                points.add(point)
            }
            pointsTextView.text = "Points:\n${points.joinToString("\n")}"
        }
        findViewById<Button>(R.id.buttonAreaCalc).setOnClickListener {
            val area = calculateArea(points)
            areaEvalTextView.text = "Area: $area"
        }
    }

    private fun calculateArea(points: MutableList<Pair<Double, Double>>): Double {
        fun cross (a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Double {
            return (b.first - a.first) * (c.second - a.second) - (b.second - a.second) * (c.first - a.first)
        }

        val n = points.size
        if (n < 3) {
            return 0.0
        }

        // points から凸包を求める
        val pointsSorted = points.sortedWith(compareBy({ it.first }, { it.second }))
        val lower = mutableListOf<Pair<Double, Double>>()
        val upper = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until n) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], pointsSorted[i]) <= 0) {
                lower.removeLast()
            }
            lower.add(pointsSorted[i])
        }
        for (i in n - 1 downTo 0) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], pointsSorted[i]) <= 0) {
                upper.removeLast()
            }
            upper.add(pointsSorted[i])
        }
        val convexHull = lower.dropLast(1) + upper.dropLast(1)

        // 凸包の面積を求める
        var area = 0.0
        for (i in convexHull.indices) {
            val j = (i + 1) % convexHull.size
            area += convexHull[i].first * convexHull[j].second - convexHull[j].first * convexHull[i].second
        }
        return abs(area) / 2
    }

    override fun onPause() {
        locationManager.removeUpdates(this)
        super.onPause()
    }

    override fun onResume() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        super.onResume()
    }

    override fun onLocationChanged(location: Location) {
        Log.d("Location", "Location changed: $location")
        latitude = location.latitude
        longitude = location.longitude
        pointNowTextView.text = "Point now: ($latitude, $longitude)"
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("Location", "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d("Location", "Provider disabled: $provider")
    }
}