package com.microbus.announcer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class SensorHelper(context: Context) : SensorEventListener {
    private var sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lastRotateDegree = 0f

    init {

        //加速度感应器
        val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        //地磁感应器
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_GAME)

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    var accelerometerValues: FloatArray = FloatArray(3)

    var magneticValues: FloatArray = FloatArray(3)


    var values: FloatArray = FloatArray(3)


    override fun onSensorChanged(event: SensorEvent) {

        // 判断当前是加速度感应器还是地磁感应器
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            //赋值调用clone方法
            accelerometerValues = event.values.clone()
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            //赋值调用clone方法
            magneticValues = event.values.clone()
        }
        val R = FloatArray(9)
        values = FloatArray(3)
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticValues)
        SensorManager.getOrientation(R, values)
//        Log.d("Main", "values[0] :" + Math.toDegrees(values[0].toDouble()))

        //values[0]的取值范围是-180到180度。
        //+-180表示正南方向，0度表示正北，-90表示正西，+90表示正东

    }

    fun getAzimuth(): Double {
        return Math.toDegrees(values[0].toDouble())
    }

}