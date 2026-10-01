package com.microbus.announcer.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.microbus.announcer.bean.Line

class LineDatabaseHelper private constructor(
    context: Context?,
    dbName: String = context?.getExternalFilesDir("")?.path + "/database/line.db"
) :
    DatabaseHelper(context, dbName, 2) {

    private val tableName = "line"

    companion object {
        @Volatile
        private var INSTANCE: LineDatabaseHelper? = null

        fun getInstance(context: Context): LineDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = LineDatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {

        val sql = "CREATE TABLE IF NOT EXISTS $tableName" + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "name VARCHAR NOT NULL," +
                "upLineStation VARCHAR NOT NULL," +
                "downLineStation VARCHAR NOT NULL," +
                "type VARCHAR DEFAULT 'B'," +
                "isRingRoute BOOLEAN DEFAULT 0," +
                "isUpAndDownInvert BOOLEAN DEFAULT 0);"
        db!!.execSQL(sql)

        Log.d(tag, "已创建表 $tableName")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        // 自 Version 2 起，新增isRingRoute
        if (oldVersion  < 2) {
            try {
                val alterSql = "ALTER TABLE $tableName ADD COLUMN isRingRoute BOOLEAN DEFAULT 0;"
                db?.execSQL(alterSql)
                Log.d(tag, "数据库升级：已添加列 isRingRoute")
            } catch (e: SQLException) {
                // 列已存在时忽略异常（保证向下兼容）
                Log.d(tag, "列 isRingRoute 已存在，跳过添加")
            }
        }

    }

    fun insert(line: Line): Long {
        val values = ContentValues()
        values.put("name", line.name)
        values.put("upLineStation", line.upLineStation)
        values.put("downLineStation", line.downLineStation)
        values.put("isUpAndDownInvert", line.isUpAndDownInvert)
        values.put("type", line.type)
        values.put("isRingRoute", line.isRingRoute)

        val result = readableDatabase.insert(tableName, null, values)
        if (result > 0)
            Log.d(tag, "已添加路线 ${line.name}")
        return result
    }

    fun delById(id: Int) {
        val db = writableDatabase
        db.delete(tableName, "id=?", arrayOf(id.toString()))
    }

    fun queryByName(name: String): List<Line> {
//        var list: MutableList<Line> = ArrayList()
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                "name=?",
                arrayOf(name),
                null,
                null,
                null
            )
        // 循环取出游标指向的每条记录
        val list = getLinesFromCursor(cursor)
        cursor.close()
        return list
    }

    fun queryById(id: Int): List<Line> {
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
        val list = getLinesFromCursor(cursor)
        cursor.close()
        return list
    }

    fun queryByCount(count: Int): List<Line> {
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                null,
                null,
                null,
                null,
                "name",
                "1"
            )
        // 循环取出游标指向的每条记录
        val list = getLinesFromCursor(cursor)
        cursor.close()
        return list
    }

    /**
     * 返回所有路线(按路线名排序)
     */
    fun queryAll(): MutableList<Line> {
        val cursor: Cursor = readableDatabase.query(tableName, null, null, null, null, null, "name")
        val list = getLinesFromCursor(cursor)
        cursor.close()
        return list
    }

    fun updateById(id: Int, line: Line) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", line.name)
            put("upLineStation", line.upLineStation)
            put("downLineStation", line.downLineStation)
            put("isUpAndDownInvert", if (line.isUpAndDownInvert) 1 else 0)
            put("type", line.type)
            put("isRingRoute", if (line.isRingRoute) 1 else 0)
        }
        db.update(tableName, values, "id=?", arrayOf(id.toString()))
    }

    fun queryByKey(key: String): List<Line> {
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                "name like ?",
                arrayOf("%${key}%"),
                null,
                null,
                null,
                null
            )
        // 循环取出游标指向的每条记录
        val list = getLinesFromCursor(cursor)
        cursor.close()
        return list
    }

    fun queryByKeyAndType(key: String, type: String): List<Line> {
        // 执行记录查询动作，该语句返回结果集的游标
        val cursor: Cursor =
            readableDatabase.query(
                tableName,
                null,
                "name like ? and type = ?",
                arrayOf("%${key}%", type),
                null,
                null,
                null,
                null
            )
        // 循环取出游标指向的每条记录
        val list = getLinesFromCursor(cursor)
        cursor.close()
        return list
    }

    fun getLinesFromCursor(cursor: Cursor): MutableList<Line> {
        // 预缓存列索引，只查一次
        val idxId = cursor.getColumnIndexOrThrow("id")
        val idxName = cursor.getColumnIndexOrThrow("name")
        val idxUp = cursor.getColumnIndexOrThrow("upLineStation")
        val idxDown = cursor.getColumnIndexOrThrow("downLineStation")
        val idxInvert = cursor.getColumnIndexOrThrow("isUpAndDownInvert")
        val idxType = cursor.getColumnIndex("type")            // 新增列，可能不存在
        val idxRing = cursor.getColumnIndex("isRingRoute")     // 新增列，可能不存在

        val list = ArrayList<Line>(cursor.count.coerceAtLeast(0))

        cursor.use { c ->
            while (c.moveToNext()) {
                list += Line(
                    id = c.getInt(idxId),
                    name = c.getString(idxName).orEmpty(),
                    upLineStation = c.getString(idxUp).orEmpty(),
                    downLineStation = c.getString(idxDown).orEmpty(),
                    isUpAndDownInvert = c.getString(idxInvert).toBooleanCompat(default = true),
                    type = if (idxType >= 0 && !c.isNull(idxType)) {
                        c.getString(idxType).orEmpty().ifEmpty { "B" }
                    } else "B",
                    isRingRoute = idxRing >= 0 && c.getInt(idxRing) == 1
                )
            }
        }
        return list
    }

    /** 兼容 "true"/"false"、"1"/"0"，默认值兜底 */
    private fun String?.toBooleanCompat(default: Boolean): Boolean =
        when (this?.trim()?.lowercase()) {
            "true", "1", "yes", "y" -> true
            "false", "0", "no", "n" -> false
            else -> default
        }
}
