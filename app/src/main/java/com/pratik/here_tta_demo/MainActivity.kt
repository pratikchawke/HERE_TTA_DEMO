package com.pratik.here_tta_demo

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.here.android.mpa.common.ApplicationContext
import com.here.android.mpa.common.MapEngine
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.guidance.TrafficUpdater
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapRoute
import com.here.android.mpa.mapping.MapView
import com.here.android.mpa.routing.*
import com.pratik.here_tta_demo.RouteUtil.RouteListener
import com.pratik.here_tta_demo.RouteUtil.createRoute


/**
 * Main activity which launches map view and handles Android run-time requesting permission.
 */
class MainActivity : AppCompatActivity() {
    private var m_mapView: MapView? = null
    private var m_map: Map? = null
    private var m_route: Route? = null
    private var m_requestInfo: TrafficUpdater.RequestInfo? = null
    private var m_coreRouter: CoreRouter? = null
    private var m_mapRoute: MapRoute? = null
    private var m_calculateRouteBtn: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        m_mapView = findViewById(R.id.mapView)
        m_calculateRouteBtn = findViewById(R.id.btnCalculateRoute)
        m_calculateRouteBtn!!.setEnabled(false)
        m_calculateRouteBtn!!.setOnClickListener(View.OnClickListener { /* Start calculating m_route */
            calculateRoute()
        })
        if (hasPermissions(
                this,
                *RUNTIME_PERMISSIONS
            )
        ) {
            initMap()
        } else {
            ActivityCompat
                .requestPermissions(
                    this,
                    RUNTIME_PERMISSIONS,
                    REQUEST_CODE_ASK_PERMISSIONS
                )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                var index = 0
                while (index < permissions.size) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index] + " not granted. " + "Please go to settings and turn on for sample app", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]+ " not granted", Toast.LENGTH_LONG).show()
                        }
                    }
                    index++
                }
                initMap()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onResume() {
        super.onResume()
        m_mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        m_mapView!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MapEngine.isInitialized()) {
            TrafficUpdater.getInstance().enableUpdate(false)
        }
        if (m_requestInfo != null) {
            /*  Cancel request by request Id */
            TrafficUpdater.getInstance().cancelRequest(m_requestInfo!!.requestId)
        }
    }

    /**
     * Initialize m_map engine.
     * After initialization set m_map object for [MapView].
     * Also in callback check if initialization completed successfully
     */
    private fun initMap() {
        MapEngine.getInstance().init(ApplicationContext(applicationContext)) { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                /* get the map object */
                m_map = Map()
                m_mapView!!.map = m_map
                m_calculateRouteBtn!!.isEnabled = true
            } else {
                AlertDialog.Builder(this@MainActivity).setMessage("Error : ${error.name}${error.details}".trimIndent())
                    .setTitle(R.string.engine_init_error)
                    .setNegativeButton(android.R.string.cancel,
                        DialogInterface.OnClickListener { dialog, which -> finish() }).create()
                    .show()
            }
        }
    }

    private fun calculateTta() {
        /*
         * Receive arrival time for the whole m_route, if you want to get time only for part of
         * m_route pass parameter in bounds 0 <= m_route.getSublegCount()
         */
        val ttaExcluding = m_route!!.getTtaExcludingTraffic(Route.WHOLE_ROUTE)
        val ttaIncluding = m_route!!.getTtaIncludingTraffic(Route.WHOLE_ROUTE)
        val tvInclude = findViewById<TextView>(R.id.tvTtaInclude)
        tvInclude.text = "Tta included: " + ttaIncluding!!.duration.toString()
        val tvExclude = findViewById<TextView>(R.id.tvTtaExclude)
        tvExclude.text = "Tta excluded: " + ttaExcluding!!.duration.toString()
    }

    private fun calculateTtaUsingDownloadedTraffic() {
        /* Turn on traffic updates */
        TrafficUpdater.getInstance().enableUpdate(true)
        m_requestInfo = TrafficUpdater.getInstance().request(
            m_route!!
        ) {
            val ttaDownloaded = m_route!!.getTtaUsingDownloadedTraffic(
                Route.WHOLE_ROUTE
            )
            runOnUiThread {
                val tvDownload = findViewById<TextView>(R.id.tvTtaDowload)
                if (tvDownload != null) {
                    tvDownload.text = "Tta downloaded: " + ttaDownloaded!!.duration.toString()
                }
            }
        }
    }

    private fun calculateRoute() {
        /* Initialize a CoreRouter */
        m_coreRouter = CoreRouter()

        /* For calculating traffic on the m_route */
        val dynamicPenalty = DynamicPenalty()
        dynamicPenalty.trafficPenaltyMode = Route.TrafficPenaltyMode.OPTIMAL
        m_coreRouter!!.setDynamicPenalty(dynamicPenalty)
        val routePlan = createRoute()
        m_coreRouter!!.calculateRoute(routePlan, object : RouteListener<List<RouteResult?>?, RoutingError?>() {

            override fun onCalculateRouteFinished(routeResults: List<RouteResult?>?, routingError: RoutingError) {
                /* Calculation is done. Let's handle the result */
                if (routingError == RoutingError.NONE) {
                    /* Get route fro results */
                    m_route = routeResults!![0]?.route

                    /* check if map route is already on map and if it is,
                            delete it.
                         */if (m_mapRoute != null) {
                        m_map!!.removeMapObject(m_mapRoute!!)
                    }

                    /* Create a MapRoute so that it can be placed on the map */m_mapRoute =
                        MapRoute(routeResults[0]!!.route)

                    /* Add the MapRoute to the map */m_map!!.addMapObject(m_mapRoute!!)

                    /*
                         * We may also want to make sure the map view is orientated properly so
                         * the entire route can be easily seen.
                         */m_map!!.zoomTo(
                        m_route!!.boundingBox!!,
                        Map.Animation.NONE,
                        15f
                    )

                    /* Get TTA */calculateTta()
                    calculateTtaUsingDownloadedTraffic()
                } else {
                    Toast.makeText(
                        applicationContext, routingError.name,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    companion object {
        private const val REQUEST_CODE_ASK_PERMISSIONS = 0x754
        private val RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        /**
         * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
         * needs when the app is running.
         */
        private fun hasPermissions(
            context: Context,
            vararg permissions: String
        ): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            }
            return true
        }
    }
}