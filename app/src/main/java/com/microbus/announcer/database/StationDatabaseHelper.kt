package com.microbus.announcer.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.microbus.announcer.bean.Station

class StationDatabaseHelper(
    context: Context?,
    dbName: String = context?.getExternalFilesDir("")?.path + "/database/station.db"
) :
    DatabaseHelper(context, dbName) {

    private val tableName = "station"

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "CREATE TABLE IF NOT EXISTS $tableName" + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "cnName VARCHAR NOT NULL," +
                "enName VARCHAR NOT NULL," +
                "longitude DOUBLE NOT NULL," +
                "latitude DOUBLE NOT NULL," +
                "type VARCHAR DEFAULT 'B');"
        db!!.execSQL(sql)
        Log.d(tag, "已创建表 $tableName")
    }

    fun insert(station: Station): Long {
        val values = ContentValues()
        values.put("cnName", station.cnName)
        values.put("enName", station.enName)
        values.put("longitude", station.longitude)
        values.put("latitude", station.latitude)
        values.put("type", station.type)

        val result = readableDatabase.insert(tableName, null, values)
        if (result > 0)
            Log.d(tag, "已添加站点 ${station.cnName}")
        else {
            Log.d(tag, "添加失败，返回码 $result")
        }
        return result
    }

    fun delById(id: Int) {
        val sql = "delete from $tableName where id = $id"
        writableDatabase.execSQL(sql)
    }

    fun queryById(id: Int): List<Station> {
        val list: MutableList<Station> = ArrayList()
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                "id=?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
        // 循环取出游标指向的每条记录
        while (cursor.moveToNext()) {
            val station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
            station.id = cursor.getInt(0)
            station.cnName = cursor.getString(1)
            station.enName = cursor.getString(2)
            station.longitude = cursor.getDouble(3)
            station.latitude = cursor.getDouble(4)
            if (!cursor.isNull(5))
                station.type = cursor.getString(5)
            list.add(station)
        }
        cursor.close()
        return list
    }

    fun queryByCnName(name: String): List<Station> {
        val list: MutableList<Station> = ArrayList()
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                "cnName=?",
                arrayOf(name),
                null,
                null,
                null
            )
        // 循环取出游标指向的每条记录
        while (cursor.moveToNext()) {
            val station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
            station.id = cursor.getInt(0)
            station.cnName = cursor.getString(1)
            station.enName = cursor.getString(2)
            station.longitude = cursor.getDouble(3)
            station.latitude = cursor.getDouble(4)
            if (!cursor.isNull(5))
                station.type = cursor.getString(5)
            list.add(station)
        }
        cursor.close()
        return list
    }

    fun queryByKey(key: String): List<Station> {
        val list: MutableList<Station> = ArrayList()
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                "(id like ?) or (cnName like ?) or (enName like ?)",
                arrayOf("%${key}%", "%${key}%", "%${key}%"),
                null,
                null,
                null,
                null
            )
        // 循环取出游标指向的每条记录
        while (cursor.moveToNext()) {
            val station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
            station.id = cursor.getInt(0)
            station.cnName = cursor.getString(1)
            station.enName = cursor.getString(2)
            station.longitude = cursor.getDouble(3)
            station.latitude = cursor.getDouble(4)
            if (!cursor.isNull(5))
                station.type = cursor.getString(5)
            list.add(station)
        }
        cursor.close()
        return list
    }

    fun queryAll(): MutableList<Station> {
        val list: MutableList<Station> = ArrayList()
        val cursor: Cursor = readableDatabase.query(tableName, null, null, null, null, null, null)
        while (cursor.moveToNext()) {
            val station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
            station.id = cursor.getInt(0)
            station.cnName = cursor.getString(1)
            station.enName = cursor.getString(2)
            station.longitude = cursor.getDouble(3)
            station.latitude = cursor.getDouble(4)
            if (!cursor.isNull(5))
                station.type = cursor.getString(5)
            list.add(station)
        }
        cursor.close()
        return list
    }

    fun updateById(id: Int, station: Station) {
        val sql =
            "update $tableName set cnName = '${station.cnName}', enName = '${station.enName}', " +
                    "longitude = ${station.longitude}, latitude = ${station.latitude}, type = '${station.type}'" +
                    "where id = $id;"
        writableDatabase.execSQL(sql)
    }
}
