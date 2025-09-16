package com.microbus.announcer

import android.content.SharedPreferences
import com.kiylx.libx.pref_component.preference_util.delegate.boolean
import com.kiylx.libx.pref_component.preference_util.delegate.int
import com.kiylx.libx.pref_component.preference_util.delegate.string

class PrefsHelper(val prefs: SharedPreferences) {
//    var isFinish by prefs.boolean("isFinish")
//    var name by prefs.string("name")
//    var age by prefs.int("age")
    var defaultLineName by prefs.string("defaultLineName")
    //***其余无关代码省略
}

