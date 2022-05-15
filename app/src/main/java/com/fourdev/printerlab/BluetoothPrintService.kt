package com.fourdev.printerlab

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothPrintService {

    // Member fields
    lateinit var mAdapter: BluetoothAdapter
    var mHandler: Handler? = null
    var mState = 0
    var mConnectThread: ConnectThread?=null
    var mConnectedThread: ConnectedThread?=null

    companion object{
        // Debugging
        private const val TAG = "BluetoothPrintService"
        private const val D = true

        // Unique UUID for this application
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Constants that indicate the current connection state
        const val STATE_NONE = 0 // we're doing nothing

        const val STATE_LISTEN = 1 // now listening for incoming connections

        const val STATE_CONNECTING = 2 // now initiating an outgoing connection

        const val STATE_CONNECTED = 3 // now connected to a remote device

    }


    /**
     * Constructor. Prepares a new Bluetooth session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    fun BluetoothPrintService(context: Context?, handler: Handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        mHandler = handler
    }

    /**
     * Set the current state of the connection
     *
     * @param state An integer defining the current connection state
     */
    @Synchronized
    private fun setState(state: Int) {
        if (D) Log.d(
            TAG,
            "setState() $mState -> $state"
        )
        mState = state
    }

    /**
     * Return the current connection state.
     */
    @Synchronized
    fun getState(): Int {
        return mState
    }

    /**
     * Start the print service. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        if (D) Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        mConnectThread?.cancel()
        mConnectThread = null
        // Cancel any thread currently running a connection
        mConnectedThread?.cancel()
        mConnectedThread = null
        setState(STATE_LISTEN)
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        if (D) Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        mConnectedThread?.cancel()
        mConnectedThread = null

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread?.start()
        setState(STATE_CONNECTING)
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        if (D) Log.d(
            TAG,
            "connected, Socket Type:$socketType"
        )

        // Cancel the thread that completed the connection
        mConnectThread?.cancel()
        mConnectThread = null

        // Cancel any thread currently running a connection
        mConnectedThread?.cancel()
        mConnectThread = null

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread?.start()

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler!!.obtainMessage(WoosimPrnMng.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(WoosimPrnMng.DEVICE_NAME, device.name)
        msg.data = bundle
        mHandler!!.sendMessage(msg)
        setState(STATE_CONNECTED)
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        if (D) Log.d(TAG, "stop")
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null
        setState(STATE_NONE)
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray?) {
        // Create temporary object
        var r: ConnectedThread

        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread!!
        }
        // Perform the write unsynchronized
        r.write(out)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // When the application is destroyed, just return
        if (mState == STATE_NONE) return

        // Send a failure message back to the Activity
        val msg = mHandler!!.obtainMessage(WoosimPrnMng.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putInt(WoosimPrnMng.TOAST, R.string.connect_fail)
        msg.data = bundle
        mHandler!!.sendMessage(msg)

        // Start the service over to restart listening mode
        start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // When the application is destroyed, just return
        if (mState == STATE_NONE) return

        // Send a failure message back to the Activity
        val msg = mHandler!!.obtainMessage(WoosimPrnMng.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putInt(WoosimPrnMng.TOAST, R.string.connect_lost)
        msg.data = bundle
        mHandler!!.sendMessage(msg)

        // Start the service over to restart listening mode
        start()
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    @SuppressLint("MissingPermission")
    inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) :
        Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String
        @SuppressLint("MissingPermission")
        override fun run() {
            Log.i(
                TAG,
                "BEGIN mConnectThread SocketType:$mSocketType"
            )
            name = "ConnectThread$mSocketType"

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG,
                        "unable to close() $mSocketType socket during connection failure", e2
                    )
                }
                Log.e(TAG, "Connection Failed", e)
                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this) { mConnectThread = null }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "close() of connect $mSocketType socket failed", e
                )
            }
        }

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = if (secure) {
                    mmDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                } else {
                    mmDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }
            mmSocket = tmp
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    inner class ConnectedThread(socket: BluetoothSocket, socketType: String) :
        Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)

                    // buffer can be over-written by next input stream data, so it should be copied
                    var rcvData: ByteArray? = ByteArray(bytes)
                    rcvData = Arrays.copyOf(buffer, bytes)

                    // Send the obtained bytes to the UI Activity
                    mHandler?.obtainMessage(WoosimPrnMng.MESSAGE_READ, bytes, -1, rcvData)?.sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "Connection Lost", e)
                    connectionLost()
                    break
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray?) {
            try {
                mmOutStream!!.write(buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmInStream!!.close()
                mmOutStream!!.close()
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }
}
