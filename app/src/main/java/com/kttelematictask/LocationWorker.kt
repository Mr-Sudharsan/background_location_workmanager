package com.kttelematictask

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kttelematictask.realm.Database
import com.kttelematictask.realm.LocationModel
import com.kttelematictask.realm.UserModel
import com.kttelematictask.utils.Constants
import com.kttelematictask.utils.PreferenceManager
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class LocationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context)
    private val _context = context
    private val preferenceManager = PreferenceManager(context)
    override fun doWork(): Result = runBlocking {
        try {
           getLocation(context = _context)
            // Handle the obtained location as needed (e.g., send it to a server)
            Result.success()
        } catch (e: Exception) {
            Log.e("", "Error getting location", e)
            Result.failure()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(context: Context) {
        val time: String =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        withContext(Dispatchers.IO) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            )
                .addOnSuccessListener { location ->
                    val appStatus = ProcessLifecycleOwner.get().lifecycle.currentState == Lifecycle.State.RESUMED
                    Log.d("Activity Status", "Activity Resume $appStatus")
//                    if (location != null && appStatus) {
//                        val updateIntent = Intent(ACTION_UPDATE_DATA).apply {
//                            putExtra(EXTRA_DATA, location)
//                        }
//                        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
//                    }
                    if (location != null ) {
                        Database.usersRealmOpen.writeBlocking {
                            val currentLocation = LocationModel().apply {
                                latitude = location.latitude
                                longitude = location.longitude
                                timeStamp = time
                                address = updateAddressText(location)
                            }
                            val userId = preferenceManager.getString(Constants.KEY_USER_ID).toString()
                            val user: UserModel? = query<UserModel>("userId == $0", userId).first().find()
                            if (user == null) {
                                println("New user")
                                val userModel = UserModel().apply {
                                    this.userId = userId
                                    locations.add(currentLocation)
                                }
                                copyToRealm(userModel)
                            } else {
                                println("Old user")
                                findLatest(user)?.locations?.add(currentLocation)
                            }
                        }
                    }
                }.addOnFailureListener {
                        Result.retry()
                }
        }

    }


    private fun updateAddressText(currentLocation: Location): String {

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
    companion object {
        private const val TAG = "LocationWorker"
        const val ACTION_UPDATE_DATA = "com.kttelematictask.ACTION_UPDATE_DATA"
        const val EXTRA_DATA = "com.kttelematictask.EXTRA_DATA"
    }
}
