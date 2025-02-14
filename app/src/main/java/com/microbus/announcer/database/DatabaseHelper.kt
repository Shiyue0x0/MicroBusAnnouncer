package com.microbus.announcer.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

open class DatabaseHelper(
    context: Context?,
    dbName: String = "",
    dbVersion: Int = 1
) : SQLiteOpenHelper(context, dbName, null, dbVersion) {

    var tag: String = javaClass.simpleName
    
    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }



}