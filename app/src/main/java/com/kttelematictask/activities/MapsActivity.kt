package com.kttelematictask.activities

import android.os.Bundle
import android.provider.CalendarContract.Colors
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.SquareCap
import com.kttelematictask.LocationAdapter
import com.kttelematictask.R
import com.kttelematictask.databinding.ActivityMapsBinding
import com.kttelematictask.realm.Database
import com.kttelematictask.realm.LocationModel
import com.kttelematictask.realm.UserModel
import com.kttelematictask.utils.Constants
import com.kttelematictask.utils.PreferenceManager
import io.realm.kotlin.ext.query

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var latLngList: ArrayList<LatLng>
    private lateinit var locationModel: LocationModel
    private lateinit var preferenceManager: PreferenceManager
    //  private lateinit var latLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        preferenceManager = PreferenceManager(this)

        if (intent != null) {
            val bundle = intent.extras
            if (bundle != null) {
                locationModel = LocationModel()
                locationModel.address = bundle.getString("address").toString()
                locationModel.latitude = bundle.getDouble("lat")
                locationModel.longitude = bundle.getDouble("lng")
                println("${bundle.getDouble("lat")}")
            }
        }

        binding.playBtn.setOnClickListener{
            getLocationDataFromRealm(preferenceManager.getString(Constants.KEY_USER_ID).toString())
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val latLng = LatLng(locationModel.latitude, locationModel.longitude)
        mMap.addMarker(MarkerOptions().position(latLng).title(locationModel.address))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    }

    private fun getLocationDataFromRealm(userId: String) {
        try {
            val locList =
                Database.usersRealmOpen.query<UserModel>("userId == $0", userId).first().find()
            latLngList = arrayListOf()
            if (locList != null) {
                for (l in locList.locations) {
                    val latLng = LatLng(l.latitude, l.longitude)
                    latLngList.add(latLng)
                }

            }
            val polylineOptions = PolylineOptions()
            polylineOptions.color(resources.getColor(R.color.black))
            polylineOptions.width(5f)
            polylineOptions.startCap(RoundCap())
            polylineOptions.endCap(RoundCap())
            polylineOptions.jointType(JointType.ROUND)
            polylineOptions.addAll(latLngList)
            mMap.clear()
            mMap.addPolyline(polylineOptions)
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }
}