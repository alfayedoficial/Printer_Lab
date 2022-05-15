package com.fourdev.printerlab

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fourdev.printerlab.DeviceListActivity.Companion.EXTRA_DEVICE_ADDRESS
import com.fourdev.printerlab.PrefMng.saveActivePrinter
import com.fourdev.printerlab.Utils.isBlueToothOn
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private val REQUEST_CONNECT = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btnThermalPrinter).setOnClickListener {
            printThermalPrinter()
        }
    }

    private fun printThermalPrinter() {
        //Check if the Bluetooth is available and on.
        if (!isBlueToothOn()) return

        saveActivePrinter(PrefMng.PRN_WOOSIM_SELECTED)

        //Pick a Bluetooth device
        val i = Intent(this, DeviceListActivity::class.java)
        startActivityForResult(i, REQUEST_CONNECT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == com.app.superpos.orders.OrderDetailsActivity.REQUEST_CONNECT && resultCode == RESULT_OK) {
            try {
                //Get device address to print to.
                val blutoothAddr =
                    data!!.extras!!.getString(EXTRA_DEVICE_ADDRESS)
                //The interface to print text to thermal printers.
                val testPrinter: IPrintToPrinter = TestPrinter(
                    this,
                    shopName,
                    shopAddress,
                    shopEmail,
                    shopContact,
                    invoiceId,
                    orderDate,
                    orderTime,
                    shortText,
                    longText,
                    orderPrice.toDouble(),
                    f.format(calculatedTotalPrice),
                    tax,
                    discount,
                    currency,
                    userName,
                    orderDetails
                )
                //Connect to the printer and after successful connection issue the print command.
                mPrnMng = printerFactory.createPrnMng(this, blutoothAddr, testPrinter)
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}