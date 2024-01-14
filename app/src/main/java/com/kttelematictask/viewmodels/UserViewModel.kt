package com.kttelematictask.viewmodels

import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kttelematictask.repository.UserRepository
import com.kttelematictask.realm.LocationModel
import kotlinx.coroutines.launch

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _userLocations = MutableLiveData<MutableList<LocationModel>>()
    val userLocations: LiveData<MutableList<LocationModel>> get() = _userLocations

    fun insertLocation(location: Location, userId: String, geocoder: Geocoder) {
        viewModelScope.launch {
            userRepository.insertLocation(location, userId, geocoder)
        }.isCompleted.let {
            loadUserLocations(userId)
        }

    }

    fun loadUserLocations(userId: String) {
        viewModelScope.launch {
            val locations = userRepository.getLocationDataFromRealm(userId)
            _userLocations.postValue(locations)
        }
    }


}
