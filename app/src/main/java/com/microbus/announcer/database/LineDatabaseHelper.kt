package com.microbus.announcer.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.microbus.announcer.bean.Line

class LineDatabaseHelper(
    context: Context?,
    dbName: String = context?.getExternalFilesDir("")?.path + "/database/line.db"
) :
    DatabaseHelper(context, dbName) {

    private val tableName = "line"

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "CREATE TABLE IF NOT EXISTS $tableName" + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "name VARCHAR NOT NULL," +
                "upLineStation VARCHAR NOT NULL," +
                "downLineStation VARCHAR NOT NULL," +
                "isUpAndDownInvert BOOLEAN NOT NULL);"
        db!!.execSQL(sql)
        Log.d(tag, "已创建表 $tableName")
    }

    fun insert(line: Line): Long {
        val values = ContentValues()
        values.put("name", line.name)
        values.put("upLineStation", line.upLineStation)
        values.put("downLineStation", line.downLineStation)
        values.put("isUpAndDownInvert", line.isUpAndDownInvert)

        val result = readableDatabase.insert(tableName, null, values)
        if (result > 0)
            Log.d(tag, "已添加路线 ${line.name}")
        return result
    }

    fun delById(id: Int) {
        val sql = "delete from $tableName where id = $id"
        writableDatabase.execSQL(sql)
    }

    fun quertByName(name: String): List<Line> {
        val list: MutableList<Line> = ArrayList()
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
        while (cursor.moveToNext()) {
            val line = Line()
            line.id = cursor.getInt(0)
            line.name = cursor.getString(1)
            line.upLineStation = cursor.getString(2)
            line.downLineStation = cursor.getString(3)
            line.isUpAndDownInvert = cursor.getString(4) == "true"
            list.add(line)
        }
        cursor.close()
        return list
    }

    fun quertByCount(count: Int): List<Line> {
        val list: MutableList<Line> = ArrayList()
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
                "1 OFFSET ${count}-1"
            )
        // 循环取出游标指向的每条记录
        while (cursor.moveToNext()) {
            val line = Line()
            line.id = cursor.getInt(0)
            line.name = cursor.getString(1)
            line.upLineStation = cursor.getString(2)
            line.downLineStation = cursor.getString(3)
            line.isUpAndDownInvert = cursor.getString(4) == "true"
            list.add(line)
        }
        cursor.close()
        return list
    }

    /**
     * 返回所有路线(按路线名排序)
     */
    fun quertAll(): MutableList<Line> {
        val list: MutableList<Line> = ArrayList()
        val cursor: Cursor = readableDatabase.query(tableName, null, null, null, null, null, "name")
        while (cursor.moveToNext()) {
            val line = Line()
            line.id = cursor.getInt(0)
            line.name = cursor.getString(1)
            line.upLineStation = cursor.getString(2)
            line.downLineStation = cursor.getString(3)
            line.isUpAndDownInvert = cursor.getString(4) == "true"
            list.add(line)
        }
        cursor.close()
        return list
    }

    fun updateById(id: Int, line: Line) {
        val sql =
            "update $tableName set name = '${line.name}', upLineStation = '${line.upLineStation}', " +
                    "downLineStation = '${line.downLineStation}', isUpAndDownInvert = '${line.isUpAndDownInvert}' " +
                    "where id = $id;"
        writableDatabase.execSQL(sql)
    }
}
