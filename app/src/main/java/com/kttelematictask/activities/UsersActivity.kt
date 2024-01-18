package com.kttelematictask.activities

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kttelematictask.LocationAdapter
import com.kttelematictask.LocationJobService
import com.kttelematictask.LocationWorker
import com.kttelematictask.databinding.ActivityUsersBinding
import com.kttelematictask.listeners.LocationClickListeners
import com.kttelematictask.realm.Database
import com.kttelematictask.realm.LocationModel
import com.kttelematictask.realm.UserModel
import com.kttelematictask.utils.Constants
import com.kttelematictask.utils.PreferenceManager
import com.kttelematictask.viewmodels.UserViewModel
import io.realm.kotlin.ext.query
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class UsersActivity : AppCompatActivity(), LocationClickListeners {

    private lateinit var binding: ActivityUsersBinding
    private var userId: String = ""
    private lateinit var locationAdapter: LocationAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var locationList: MutableList<LocationModel>
    private val userViewModel: UserViewModel by viewModel()
    private lateinit var geocoder: Geocoder
    private lateinit var jobScheduler: JobScheduler
    private lateinit var locationBroadcastReceiver: LocationBroadcastReceiver
    private val resolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                if (!preferenceManager.getBoolean(Constants.IS_SERVICE_RUNNING)) {
                    //scheduleJob()
                     initializeWorker()
                }
            } else {
            }
        }


    private val requestFineLocationLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Background location permission")
                    .setMessage("Please choose Always allow all the time for continuous location tracking.")
                    .setPositiveButton("ok") { dialog, _ ->
                        checkBackgroundLocationPermissions()
                        dialog.cancel()
                    }.show()
            } else {

            }
        }
    private val requestBackgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                checkGps()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(this)
        locationList = mutableListOf()
        geocoder = Geocoder(this)
        locationBroadcastReceiver = LocationBroadcastReceiver()
        jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        //    locationBroadcastReceiver = LocationBroadcastReceiver()
        if (intent.hasExtra("userId")) {
            userId = intent.getStringExtra("userId").toString()
            println("User id : $userId")
        }

        binding.imageBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.logoutBtn.setOnClickListener {
            preferenceManager.putBoolean(Constants.IS_LOGGED_IN, false)
            preferenceManager.putBoolean(Constants.IS_SERVICE_RUNNING, false)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver)
            WorkManager.getInstance(this).cancelAllWorkByTag(userId)
            val intent = Intent(this@UsersActivity, SignInActivity::class.java)
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                println("Back button pressed")
                MaterialAlertDialogBuilder(this@UsersActivity).setTitle("Exit")
                    .setMessage("Are you sure want to exit")
                    .setPositiveButton("ok") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }.setNegativeButton("cancel") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        })

        binding.username.text = userId

        userViewModel.loadUserLocations(userId = userId)

        userViewModel.userLocations.observe(this) { locationList ->
            locationAdapter = LocationAdapter(locationList, this)
            binding.locationRecyclerView.adapter = locationAdapter
            binding.locationRecyclerView.visibility = View.VISIBLE
        }
        checkAndRequestPermissions()
        //getLocationDataFromRealm(userId)
    }

    private fun scheduleJob() {
        val componentName = ComponentName(this, LocationJobService::class.java)
        val jobInfoBuilder = JobInfo.Builder(101, componentName)
        // Set the desired interval (15 minutes in milliseconds)
        jobInfoBuilder.setPeriodic(15 * 60 * 1000)
            .setPersisted(true)
        // Specify additional constraints or settings if needed
        // For example, setRequiredNetworkType, setRequiresCharging, etc.
        jobScheduler.schedule(jobInfoBuilder.build())
        preferenceManager.putBoolean(Constants.IS_SERVICE_RUNNING, true)
    }

    private fun initializeWorker() {
        // Create constraints to ensure the work only runs when conditions are met
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false)
            .build()

        // Create the work request
        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10,TimeUnit.SECONDS
                )
                .addTag(userId)
                .build()

        // Enqueue the work request

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            userId,
            ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest
        )
        val workInfoLiveData: LiveData<List<WorkInfo>> =
            WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(Constants.LOCATION_WORK_MANGER)

        workInfoLiveData.observe(this) { workInfoList ->
            if (workInfoList.isNotEmpty()) {
                val workInfo = workInfoList[0]
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        println("WorkInfo State SUCCEEDED")
                    }

                    WorkInfo.State.BLOCKED -> {
                        println("WorkInfo State BLOCKED")
                    }

                    WorkInfo.State.ENQUEUED -> {
                        println("WorkInfo State ENQUEUED")
                    }

                    WorkInfo.State.FAILED -> {
                        println("WorkInfo State FAILED")
                    }

                    WorkInfo.State.RUNNING -> {
                        println("WorkInfo State RUNNING")
                    }

                    WorkInfo.State.CANCELLED -> {
                        println("WorkInfo State CANCELLED")
                    }
                }
            }
        }
        preferenceManager.putBoolean(Constants.IS_SERVICE_RUNNING, true)

    }

    private fun getLocationDataFromRealm(userId: String) {
        val locList =
            Database.usersRealmOpen.query<UserModel>("userId == $0", userId).first().find()
        if (locList != null) {
            for (l in locList.locations) {
                println("latLng : ${l.latitude} ,${l.longitude} Address : ${l.address}")
            }
            locationAdapter = LocationAdapter(locList.locations, this)
            binding.locationRecyclerView.adapter = locationAdapter
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestFineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkBackgroundLocationPermissions()
        }
    }

    private fun checkBackgroundLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            checkGps()
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationBroadcastReceiver,
            IntentFilter(LocationWorker.ACTION_UPDATE_DATA)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        println("destroy called...")

    }

    private fun checkGps() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        val task =
            LocationServices.getSettingsClient(this@UsersActivity)
                .checkLocationSettings(builder.build())

        task.addOnSuccessListener { response ->
            val states = response.locationSettingsStates
            if (states!!.isLocationPresent) {
                if (!preferenceManager.getBoolean(Constants.IS_SERVICE_RUNNING)) {
                    //scheduleJob()
                    initializeWorker()
                }

            }
        }
            .addOnFailureListener { e ->
                val statusCode = (e as ResolvableApiException).statusCode
                val resolvableApiException = e as ResolvableApiException
                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(resolvableApiException.resolution).build()
                    resolutionForResult.launch(intentSenderRequest)
                    resolvableApiException.startResolutionForResult(this@UsersActivity, 101)
                }
            }
    }

    override fun onLocationClicked(locationModel: LocationModel) {
        val intent = Intent(this@UsersActivity, MapsActivity::class.java)
        val bundle = Bundle()
        bundle.putDouble("lat", locationModel.latitude)
        bundle.putDouble("lng", locationModel.longitude)
        bundle.putString("address", locationModel.address)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private inner class LocationBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                LocationWorker.EXTRA_DATA
            )
            if (location != null) {
                userViewModel.insertLocation(location, userId, geocoder)
            }
            Log.d("TAG..", "Current location in activity : $location")
        }
    }


}