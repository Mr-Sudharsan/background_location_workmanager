package com.kttelematictask.repository

import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.kttelematictask.realm.Database
import com.kttelematictask.realm.LocationModel
import com.kttelematictask.realm.UserModel
import io.realm.kotlin.ext.query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserRepository {

    fun insertLocation(location: Location,userId :String,geocoder :Geocoder){

        val time: String =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        Database.usersRealmOpen.writeBlocking {
            val currentLocation = LocationModel().apply {
                latitude = location.latitude
                longitude = location.longitude
                timeStamp = time
                address = updateAddressText(location,geocoder)
            }
           val user: UserModel? =
                query<UserModel>("userId == $0", userId).first().find()

            if (user == null) {
                Log.d(TAG, "New User")
                val userModel = UserModel().apply {
                    this.userId = userId
                    locations.add(currentLocation)
                }
                copyToRealm(userModel)
            } else {
                Log.d(TAG, "Old User")
                findLatest(user)?.locations?.add(currentLocation)
            }
        }

    }
    private fun updateAddressText(currentLocation: Location,geocoder :Geocoder): String {

        val latitude = currentLocation.latitude
        val longitude = currentLocation.longitude
        val addressText = StringBuilder()
        return geocoder.getFromLocation(latitude, longitude, 1)?.let { addresses ->
            val address = addresses[0]
            addressText.append(address.locality).append(", ")
            addressText.append(address.adminArea).append(", ")
            addressText.append(address.countryName).append(", ")
            addressText.toString()
        } ?: ""
    }

     fun getLocationDataFromRealm(userId: String): MutableList<LocationModel> {
        var locationList : MutableList<LocationModel> = mutableListOf()
        val locList =
            Database.usersRealmOpen.query<UserModel>("userId == $0", userId).first().find()
        if (locList != null) {
             locationList = locList.locations
        }
        return locationList
    }

    companion object {
        private const val TAG = "LocationRepository"
    }
}