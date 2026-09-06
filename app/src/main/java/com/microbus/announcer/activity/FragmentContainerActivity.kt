package com.microbus.announcer.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.microbus.announcer.R

class FragmentContainerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FRAGMENT_CLASS = "fragment_class"
        const val EXTRA_FRAGMENT_ARGS = "fragment_args"

        fun start(context: Context, fragmentClass: Class<out Fragment>, args: Bundle? = null) {
            val intent = Intent(context, FragmentContainerActivity::class.java)
            intent.putExtra(EXTRA_FRAGMENT_CLASS, fragmentClass.name)
            intent.putExtra(EXTRA_FRAGMENT_ARGS, args)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)

        if (savedInstanceState == null) {
            val fragmentClass = intent.getStringExtra(EXTRA_FRAGMENT_CLASS)
            val args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS)

            if (fragmentClass != null) {
                try {
                    val fragment = Class.forName(fragmentClass).newInstance() as Fragment
                    fragment.arguments = args
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}