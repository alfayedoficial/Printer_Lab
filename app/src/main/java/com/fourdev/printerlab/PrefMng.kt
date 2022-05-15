package com.fourdev.printerlab

import android.content.Context
import com.woosim.printer.WoosimCmd

object PrefMng {

    private val PREF_NAME = "org.kasabeh.androidprint"
    private val PREF_DEV_ADDR = "PrefMng.PREF_DEVADDR"
    private val PREF_PRINTER = "PrefMng.PREF_PRINTER"
    val PRN_WOOSIM_SELECTED = 1
    val PRN_BIXOLON_SELECTED = 2
    val PRN_OTHER_PRINTERS_SELECTED = 3
    val PRN_RONGTA_SELECTED = 4

    fun Context.getActivePrinter(): Int {
        val pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getInt(PREF_PRINTER, PRN_WOOSIM_SELECTED)
    }

    fun Context.saveActivePrinter(printerName: Int) {
        val pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putInt(PREF_PRINTER, printerName)
        editor.apply()
    }

    fun Context.saveDeviceAddr(newAddr: String?) {
        val pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(PREF_DEV_ADDR, newAddr)
        editor.apply()
    }

    /**
     * You can use the getDeviceAddr method to bypass DeviceListActivity.
     * @param context
     * @return If the return value is an empty string, it means no printer Bluetooth
     * address already is saved. In this case you MUST first run DeviceListActivity.
     * If the return value is not empty then you can bypass loading DeviceListActivity.
     * The best place to save the Bluetooth address is in the IPrintToPrinter.printEnded
     * method when the print operation is ended successfully.
     */
    fun Context.getDeviceAddr(): String? {
        val pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(PREF_DEV_ADDR, "")
    }

    fun Context.getBoldPrinting(): Boolean {
        return false
    }

    /**
     * This method is specific to the Woosim printers only. In other words,
     * you can choose which font to use on the Woosim printers.
     * @return The code table for printing.
     */
    fun getWoosimCodeTbl(): Int {
        /*Based on the installed font on the device you can return
          WoosimCmd.CT_IRAN_SYSTEM or other code tables.*/
        return WoosimCmd.CT_ARABIC_FARSI //It also supports English.
    }
}