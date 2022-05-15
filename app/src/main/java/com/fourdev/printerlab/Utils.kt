package com.fourdev.printerlab

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.Toast

object Utils {

    @SuppressLint("MissingPermission")
    fun Context.isBlueToothOn():Boolean{
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show()
            return false
        }
        if (!mBluetoothAdapter.isEnabled) {
            Toast.makeText(this, getString(R.string.turn_on_bluetooth), Toast.LENGTH_LONG).show()
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableIntent)
            return false
        }
        return true
    }
}