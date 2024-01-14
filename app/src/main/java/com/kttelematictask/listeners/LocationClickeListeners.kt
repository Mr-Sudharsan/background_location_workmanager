package com.kttelematictask.listeners

import com.kttelematictask.realm.LocationModel

interface LocationClickListeners {
    fun onLocationClicked(locationModel: LocationModel)
}