package com.annasblackhat.printtousb

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbConstants
import android.os.Build
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private val ACTION_USB_PERMISSION = "com.annasblackhat.printtousb.USB_PERMISSION"
    private var mConnection: UsbDeviceConnection? = null
    private var mInterface: UsbInterface? = null
    private var mEndPoint: UsbEndpoint? = null
    private var mDevice: UsbDevice? = null
    private lateinit var usbManager: UsbManager
    private val usbList = ArrayList<UsbDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList

        if (deviceList.size > 0) {
            Toast.makeText(this, "Printer Size : ${deviceList.size}", Toast.LENGTH_SHORT).show()
            val deviceIterator = deviceList.values.iterator()

            var usbDeviceInfo = ""

            usbDeviceInfo += "${Build.BRAND} - ${Build.MODEL} - ${Build.MANUFACTURER} - ${Build.BOARD}\n"

            for ((k, v) in deviceList) {
                usbDeviceInfo += "$k - "
            }

            usbList.clear()
            while (deviceIterator.hasNext()) {
                val usbDevice = deviceIterator.next()
                usbList.add(usbDevice)
                usbDeviceInfo += "\n\n" +
                        "Device ID       : ${usbDevice.deviceId} \n" +
                        "Device Name     : ${usbDevice.deviceName} \n" +
                        "Protocol        : ${usbDevice.deviceProtocol} \n" +
                        "Product Name    : ${usbDevice.productName} \n" +
                        "Manufacture Name: ${usbDevice.manufacturerName} \n" +
                        "Device Class    : ${usbDevice.deviceClass} - ${translateDeviceClass(usbDevice.deviceClass)} \n" +
                        "Device Subclass : ${usbDevice.deviceSubclass} \n" +
                        "Vendor ID       : ${usbDevice.vendorId} \n" +
                        "Product ID      : ${usbDevice.productId}\n" +
                        "Version         : ${usbDevice.version}\n" +
                        "Serial Number   : ${usbDevice.serialNumber}\n"

                var interfaceCount = usbDevice.interfaceCount
//                toas("INTERFACE COUNT : $interfaceCount")

//                if (usbDevice.deviceId == 1004) {
//                    mDevice = usbDevice
//                }

//                toas("Device is attatched")
            }
            txt_usbDevice.text = usbDeviceInfo

            mDevice?.let {
                ed_txt.setText("Test print....")
                requestPermission(it)
            }

        } else {
            showMsg("Please attach printer via USB and restart the app")
        }

        btn_print.setOnClickListener {
            //            printText(ed_txt.text.toString(), mConnection, mInterface)
            chooseUsb()
        }
    }

    private fun chooseUsb() {
        if (usbList.isEmpty()) {
            showMsg("No bluetooth detected!")
            return
        }

        val usbNames = ArrayList<String>()
        usbList.forEach { usbNames.add("${it.deviceId} - ${it.deviceName}") }
        AlertDialog.Builder(this)
            .setTitle("Choose Device")
            .setSingleChoiceItems(usbNames.toTypedArray(), 0, null)
            .setPositiveButton("Print") { dialog, _ ->
                val position = (dialog as AlertDialog).listView.checkedItemPosition
//                toas("Selected position : $position | ${usbList[position].vendorId}")
                requestPermission(usbList[position])
            }
            .show()
    }

    private fun requestPermission(usbDevice: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val intentFilter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, intentFilter)
        usbManager.requestPermission(usbDevice, permissionIntent)
    }

    private fun printText(
        message: String,
        connection: UsbDeviceConnection?,
        usbInterface: UsbInterface?,
        mEndPoint: UsbEndpoint?
    ) {
        when {
            usbInterface == null -> showMsg("INTERFACE IS NULL")
            connection == null -> showMsg("CONNECTION IS NULL")
            else -> {
                connection?.claimInterface(usbInterface, true)
                val testBytes = "$message \n\n ".toByteArray()
                val cut_paper = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
//                val thread = Thread(Runnable {
//                    //                    val cut_paper = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
//                    connection.bulkTransfer(mEndPoint, testBytes, testBytes.size, 0)
////                    connection.bulkTransfer(mEndPoint, cut_paper, cut_paper.size, 0)
//                })
//                thread.run()
                val totalByte = ByteArray(testBytes.size + cut_paper.size)
                System.arraycopy(testBytes, 0, totalByte, 0, testBytes.size)
                System.arraycopy(cut_paper, 0, totalByte, testBytes.size, cut_paper.size)

//                toas("Printing... $message")
                thread(true) {
                    try {
                        val res = connection.bulkTransfer(mEndPoint, totalByte, totalByte.size, 1000)
                        println("Print finish... $res")
                        toas("Print finish : $res")
                    } catch (e: Exception) {
                        println("print error $e")
                    }
//                    connection.bulkTransfer(mEndPoint, cut_paper, cut_paper.size, 0)
//                    connection.bulkTransfer(mEndPoint, totalByte, totalByte.size, 0)
                }
//                PrinterManager().print(message, connection, mEndPoint, usbInterface)
            }
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            val action = intent?.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent?.getParcelableExtra<UsbDevice?>(UsbManager.EXTRA_DEVICE)
                    if (intent?.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            val mInterface = getInterface(0)
                            if ((mInterface?.endpointCount ?: 0) > 1) {
                                toas("Endpoint size ${mInterface?.endpointCount} | interface count : $interfaceCount")
//                                mEndPoint = mInterface?.getEndpoint(0)
//                                showMsg("$mEndPoint")
//                                mConnection = usbManager.openDevice(this)
//                                printText(ed_txt.text.toString(), mConnection, mInterface)

                                val max = mInterface?.endpointCount ?: 0

                                var log = ""
                                for (i in 0 until max) {
                                    val endp = mInterface?.getEndpoint(i)
                                    if(endp?.type == UsbConstants.USB_ENDPOINT_XFER_BULK){
                                        if(endp?.direction == UsbConstants.USB_DIR_OUT){
                                            log += "print to endpoint : $i"
                                            val mConnection = usbManager.openDevice(this)
                                            printText(ed_txt.text.toString(), mConnection, mInterface, endp)
                                            break
                                        }
                                    }
                                }
                                txt_usbDevice.text = log
                            } else {
                                toas("Device has not enough endpoint")
                            }
                        }
                    } else {
                        toas("PERMISSION DENIED FOR THIS DEVICE")
                    }
                }
            }
        }
    }

    private fun showMsg(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Message")
            .setMessage(msg)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun toas(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private fun translateDeviceClass(deviceClass: Int) = when (deviceClass) {

        UsbConstants.USB_CLASS_APP_SPEC -> "Application specific USB class"

        UsbConstants.USB_CLASS_AUDIO -> "USB class for audio devices"

        UsbConstants.USB_CLASS_CDC_DATA -> "USB class for CDC devices (communications device class)"

        UsbConstants.USB_CLASS_COMM -> "USB class for communication devices"

        UsbConstants.USB_CLASS_CONTENT_SEC -> "USB class for content security devices"

        UsbConstants.USB_CLASS_CSCID -> "USB class for content smart card devices"

        UsbConstants.USB_CLASS_HID -> "USB class for human interface devices (for example, mice and keyboards)"

        UsbConstants.USB_CLASS_HUB -> "USB class for USB hubs"

        UsbConstants.USB_CLASS_MASS_STORAGE -> "USB class for mass storage devices"

        UsbConstants.USB_CLASS_MISC -> "USB class for wireless miscellaneous devices"

        UsbConstants.USB_CLASS_PER_INTERFACE -> "USB class indicating that the class is determined on a per-interface basis"

        UsbConstants.USB_CLASS_PHYSICA -> "USB class for physical devices"

        UsbConstants.USB_CLASS_PRINTER -> "USB class for printers"

        UsbConstants.USB_CLASS_STILL_IMAGE -> "USB class for still image devices (digital cameras)"

        UsbConstants.USB_CLASS_VENDOR_SPEC -> "Vendor specific USB class"

        UsbConstants.USB_CLASS_VIDEO -> "USB class for video devices"

        UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "USB class for wireless controller devices"

        else -> "Unknown USB class!"
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }
}

