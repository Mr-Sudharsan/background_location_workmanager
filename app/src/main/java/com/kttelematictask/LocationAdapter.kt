package com.kttelematictask

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kttelematictask.databinding.LocationListItemBinding
import com.kttelematictask.listeners.LocationClickListeners
import com.kttelematictask.realm.LocationModel


class LocationAdapter(private var locationList: List<LocationModel>,private val locationClickListeners: LocationClickListeners) :
    RecyclerView.Adapter<LocationAdapter.MyViewHolder>() {

    class MyViewHolder(private var binding: LocationListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setUserData(locationModel: LocationModel, locationClickListeners: LocationClickListeners) {
            binding.latLangTxt.text = "${locationModel.latitude} , ${locationModel.longitude}"
            binding.addressTxt.text = locationModel.address
            binding.timeStampTxt.text=locationModel.timeStamp
            binding.viewBtn.setOnClickListener {
                locationClickListeners.onLocationClicked(locationModel)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemContainerUserBinding = LocationListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MyViewHolder(itemContainerUserBinding)
    }

    override fun getItemCount(): Int {
        return locationList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setUserData(locationList[position],locationClickListeners)
    }


}
