package com.annasblackhat.printtousb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.AsyncTask;

/**
 * Created by annasblackhat on 18/12/18
 */
public class PrinterManager {
    public void print(String text, final UsbDeviceConnection connection, final UsbEndpoint endpoint, UsbInterface usbInterface){
        final byte[] data = (text + "\n\n\n").getBytes();
        connection.claimInterface(usbInterface, true);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                    connection.bulkTransfer(endpoint, data, data.length, 0);
            }
        });
        thread.run();

//        new AsyncTask<Void, Void, Void>(){
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                connection.bulkTransfer(endpoint, data, data.length, 0);
//                return null;
//            }
//        }.execute();
    }

}
