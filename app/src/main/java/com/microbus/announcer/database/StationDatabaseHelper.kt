package com.microbus.announcer.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.microbus.announcer.bean.Station

class StationDatabaseHelper private constructor(
    context: Context?,
    dbName: String = context?.getExternalFilesDir("")?.path + "/database/station.db"
) :
    DatabaseHelper(context, dbName, 2) {

    private val tableName = "station"

    companion object {
        @Volatile
        private var INSTANCE: StationDatabaseHelper? = null

        fun getInstance(context: Context): StationDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = StationDatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "CREATE TABLE IF NOT EXISTS $tableName" + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "cnName VARCHAR NOT NULL," +
                "enName VARCHAR NOT NULL," +
                "longitude DOUBLE NOT NULL," +
                "latitude DOUBLE NOT NULL," +
                "type VARCHAR DEFAULT 'B'," +
                "bearing DOUBLE DEFAULT -1.0);"
        db!!.execSQL(sql)
        Log.d(tag, "已创建表 $tableName")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        // 自 Version 2 起，新增bearing
        if (oldVersion < 2) {
            try {
                val alterSql = "ALTER TABLE $tableName ADD COLUMN bearing DOUBLE DEFAULT 0.0;"
                db?.execSQL(alterSql)
                Log.d(tag, "数据库升级：已添加列 bearing")
            } catch (e: SQLException) {
                // 列已存在时忽略异常（保证向下兼容）
                Log.d(tag, "列 bearing 已存在，跳过添加")
            }
        }

    }

    fun insert(station: Station): Long {
        val values = ContentValues()
        values.put("cnName", station.cnName)
        values.put("enName", station.enName)
        values.put("longitude", station.longitude)
        values.put("latitude", station.latitude)
        values.put("type", station.type)
        values.put("bearing", station.bearing)

        val result = readableDatabase.insert(tableName, null, values)
        if (result > 0)
            Log.d(tag, "已添加站点 ${station.cnName}")
        else {
            Log.d(tag, "添加失败，返回码 $result")
        }
        return result
    }


    fun delById(id: Int) {
        val db = writableDatabase
        db.delete(tableName, "id=?", arrayOf(id.toString()))
    }

    fun queryById(id: Int): List<Station> {
//        var list: MutableList<Station> = ArrayList()
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
        val list = getStationsFromCursor(cursor)
        cursor.close()
        return list
    }

    fun queryByCnName(name: String): List<Station> {
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
        val list = getStationsFromCursor(cursor)
        cursor.close()
        return list
    }

    fun queryByKey(key: String): List<Station> {
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
        val list = getStationsFromCursor(cursor)
        cursor.close()
        return list
    }


    fun queryByCount(count: Int, key: String): Station {

        val selection = if (key != "")
            "(id like ?) or (cnName like ?) or (enName like ?)"
        else
            null

        val selectionArgs = if (key != "")
            arrayOf("%${key}%", "%${key}%", "%${key}%")
        else
            null


        val cursor: Cursor = readableDatabase.query(
            tableName, null, selection,
            selectionArgs, null, null, null
        )
        var cursorCount = 0
        val station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)

        val idxId = cursor.getColumnIndexOrThrow("id")
        val idxCnName = cursor.getColumnIndexOrThrow("cnName")
        val idxEnName = cursor.getColumnIndexOrThrow("enName")
        val idxLongitude = cursor.getColumnIndexOrThrow("longitude")
        val idxLatitude = cursor.getColumnIndexOrThrow("latitude")
        val idxType = cursor.getColumnIndexOrThrow("type")
        val idxBearing = cursor.getColumnIndexOrThrow("bearing")

        while (cursor.moveToNext()) {
            if (count == cursorCount) {
                return Station(
                    id = cursor.getInt(idxId),
                    cnName = cursor.getString(idxCnName) ?: "",
                    enName = cursor.getString(idxEnName) ?: "",
                    longitude = cursor.getDouble(idxLongitude),
                    latitude = cursor.getDouble(idxLatitude),
                    type = if (cursor.isNull(idxType)) "B" else cursor.getString(idxType) ?: "B",
                    bearing = if (cursor.isNull(idxBearing)) -1.0 else cursor.getDouble(idxBearing)
                )
            }
            cursorCount++
        }
        cursor.close()
        return station

    }

    fun queryAll(): MutableList<Station> {
        val cursor: Cursor = readableDatabase.query(tableName, null, null, null, null, null, null)
        val list = getStationsFromCursor(cursor)
        cursor.close()
        return list
    }

    fun queryByTypes(types: BooleanArray): MutableList<Station> {

        val typeNameListTotal = arrayOf("C", "B", "U", "T")
        val typeNameList = typeNameListTotal
            .filterIndexed { index, _ -> types[index] }
        val typeNameSelection = typeNameList
            .joinToString(separator = " or ") { "(type='$it')" }

        val cursor: Cursor = readableDatabase.query(
            tableName,
            null,
            typeNameSelection,
            null,
            null,
            null,
            null
        )
        val list = getStationsFromCursor(cursor)
        cursor.close()
        return list
    }

    fun updateById(id: Int, station: Station) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("cnName", station.cnName)
            put("enName", station.enName)
            put("longitude", station.longitude)
            put("latitude", station.latitude)
            put("type", station.type)
            put("bearing", station.bearing)
        }
        db.update(tableName, values, "id=?", arrayOf(id.toString()))
    }

    fun getCount(): Long {
        val db = readableDatabase
        var count: Long = 0

        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return count
    }

    fun getCountByKey(key: String): Long {
        val db = readableDatabase
        var count: Long = 0

        val where = if (key != "")
            "WHERE id LIKE '%$key%' OR cnName LIKE '%$key%' OR enName LIKE '%$key%'"
        else
            ""

        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $tableName $where",
                null
            )
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return count
    }

    fun getStationsFromCursor(cursor: Cursor): MutableList<Station> {
        // 提前获取列索引，避免循环内重复查找
        val idxId = cursor.getColumnIndexOrThrow("id")
        val idxCnName = cursor.getColumnIndexOrThrow("cnName")
        val idxEnName = cursor.getColumnIndexOrThrow("enName")
        val idxLongitude = cursor.getColumnIndexOrThrow("longitude")
        val idxLatitude = cursor.getColumnIndexOrThrow("latitude")
        val idxType = cursor.getColumnIndexOrThrow("type")
        val idxBearing = cursor.getColumnIndexOrThrow("bearing")

        val list = ArrayList<Station>(cursor.count)
        while (cursor.moveToNext()) {
            list.add(
                Station(
                    id = cursor.getInt(idxId),
                    cnName = cursor.getString(idxCnName) ?: "",
                    enName = cursor.getString(idxEnName) ?: "",
                    longitude = cursor.getDouble(idxLongitude),
                    latitude = cursor.getDouble(idxLatitude),
                    type = if (cursor.isNull(idxType)) "B" else cursor.getString(idxType) ?: "B",
                    bearing = if (cursor.isNull(idxBearing)) -1.0 else cursor.getDouble(idxBearing)
                )
            )
        }
        return list
    }
}
