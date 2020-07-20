package com.pratik.here_tta_demo

import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.routing.RouteOptions
import com.here.android.mpa.routing.RoutePlan
import com.here.android.mpa.routing.RouteWaypoint
import com.here.android.mpa.routing.Router


object RouteUtil {
    fun createRoute(): RoutePlan {
        /* Initialize a RoutePlan */
        val routePlan = RoutePlan()

        /*
         * Initialize a RouteOption. HERE Mobile SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        val routeOptions = RouteOptions()
        /* Other transport modes are also available e.g Pedestrian */routeOptions.transportMode =
            RouteOptions.TransportMode.CAR
        /* Disable highway in this route. */routeOptions.setHighwaysAllowed(false)
        /* Calculate the shortest route available. */routeOptions.routeType =
            RouteOptions.Type.FASTEST
        /* Calculate 1 route. */routeOptions.routeCount = 1
        /* Finally set the route option */routePlan.routeOptions = routeOptions

        /* Define waypoints for the route */
        /* START: 401 Franklin St, Houston, TX 77201, United States*/
        val startPoint = RouteWaypoint(
            GeoCoordinate(29.766173, -95.364624)
        )

        /* MIDDLE: */
//        val middlePoint = RouteWaypoint(
//            GeoCoordinate()
//        )

        /* END: Waste Management Inc, Main street, Houston, TX, USA */
        val destination = RouteWaypoint(
            GeoCoordinate(29.756607, -95.365483)
        )

        /* Add both waypoints to the route plan */
        routePlan.addWaypoint(startPoint)
//        routePlan.addWaypoint(middlePoint)
        routePlan.addWaypoint(destination)
        return routePlan
    }

    internal abstract class RouteListener<T, U : Enum<*>?> :
        Router.Listener<T, U> {
        override fun onProgress(i: Int) {
            /* The calculation progress can be retrieved in this callback. */
        }
    }
}