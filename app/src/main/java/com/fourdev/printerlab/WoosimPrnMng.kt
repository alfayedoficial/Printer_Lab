package com.fourdev.printerlab

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.woosim.printer.WoosimService

object WoosimPrnMng {

    const val MESSAGE_DEVICE_NAME = 1
    const val MESSAGE_TOAST = 2
    const val MESSAGE_READ = 3

    // Key names received from the BluetoothPrintService Handler
    const val DEVICE_NAME = "device_name"
    const val TOAST = "toast"
    //public static final int CActiveCodeTable = WoosimCmd.CT_ARABIC_FARSI;
    //public static final int CActiveCodeTable = WoosimCmd.CT_ARABIC_FORMS_B;

    //public static final int CActiveCodeTable = WoosimCmd.CT_ARABIC_FARSI;
    //public static final int CActiveCodeTable = WoosimCmd.CT_ARABIC_FORMS_B;
    private val mPrintService: BluetoothPrintService? = null
    private val mWoosim: WoosimService? = null
    private const val mDeviceAddr = ""
    private val device: BluetoothDevice? = null
    private val mPrintToPrinter: IPrintToPrinter? = null
}