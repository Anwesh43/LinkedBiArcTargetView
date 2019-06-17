package com.anwesh.uiprojects.linkedbiarctargetview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.biarctargetview.BiArcTargetView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiArcTargetView.create(this)
    }
}
