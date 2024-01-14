package com.kttelematictask.realm

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.io.Serializable

open class LocationModel : RealmObject {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var address: String = ""
    var timeStamp :String =""
}

open class UserModel : RealmObject {
    @PrimaryKey
    var userId: String = ""
    var locations: RealmList<LocationModel> = realmListOf()
}

class User: RealmObject{
    @PrimaryKey
    var userId: String = ""
    var password: String = ""
    var email: String = ""
}

