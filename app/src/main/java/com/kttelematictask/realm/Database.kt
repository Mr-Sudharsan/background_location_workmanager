package com.kttelematictask.realm

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object Database {

    private val config = RealmConfiguration.Builder(schema = setOf(User::class,UserModel::class,LocationModel::class))
        .schemaVersion(1) // Update schema version as needed
        .build()

    val usersRealmOpen: Realm = Realm.open(config)

}