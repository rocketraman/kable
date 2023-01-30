package com.juul.kable

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ERROR
import android.bluetooth.BluetoothAdapter.EXTRA_STATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

private val bluetoothStateIntentFilter: IntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

internal typealias BluetoothAdapterStateListener = (state: Int) -> Unit

internal object BluetoothAdapterState {

    private val listeners = mutableSetOf<BluetoothAdapterStateListener>()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(EXTRA_STATE, ERROR)
            notifyListeners(state)
        }
    }

    fun addListener(listener: BluetoothAdapterStateListener) {
        synchronized(listeners) {
            listeners += listener
            if (listeners.count() == 1) {
                applicationContext.registerReceiver(receiver, bluetoothStateIntentFilter)
            }
        }
    }

    fun removeListener(listener: BluetoothAdapterStateListener) {
        synchronized(listeners) {
            listeners -= listener
            if (listeners.isEmpty()) {
                applicationContext.unregisterReceiver(receiver)
            }
        }
    }

    private fun notifyListeners(state: Int) {
        synchronized(listeners) {
            listeners.forEach {
                it.invoke(state)
            }
        }
    }
}
