/*
 * Copyright (c) by Michał Niedźwiecki 2016
 * Contact: nkg753 on gmail or via GitHub profile: dzwiedziu-nkg
 *
 * This file is part of Bike Road Quality.
 *
 * Bike Road Quality is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bike Road Quality is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package pl.nkg.brq.android.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public abstract class AbstractBluetoothCommunication {

    private static final String TAG = "Bluetooth";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    //private ConnectThread mConnectThread;
    private ConnectTask mConnectTask;
    private ReceiveTask mReceiveTask;
    private int mState;
    private BluetoothDevice mDeviceWithConnected;
    private boolean mConnectionSecureType;
    private boolean mReconnect;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    /**
     * Constructor. Prepares a new BluetoothChat session.
     */
    public AbstractBluetoothCommunication() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mReconnect = true;
    }

    private void setState(int state) {
        mState = state;
    }

    public int getState() {
        return mState;
    }

    public void connect(String address, boolean secure) {
        connect(mAdapter.getRemoteDevice(address), secure);
    }

    public void connect(BluetoothDevice device, boolean secure) {
        mReconnect = true;
        doConnect(device, secure);
    }

    private void doConnect(BluetoothDevice device, boolean secure) {
        mDeviceWithConnected = device;
        mConnectionSecureType = secure;

        if (!mAdapter.isEnabled()) {
            //Log("Bluetooth is not enabled!");
            return;
        }
        cancelTasks();


        //mConnectThread = new ConnectThread(device, secure);
        //mConnectThread.start();
        mConnectTask = new ConnectTask(secure);
        mConnectTask.execute(device);
        setState(STATE_CONNECTING);
    }

    private void cancelTasks() {
        // Cancel any thread attempting to make a connection
        if (mConnectTask != null) {
            mConnectTask.cancel(true);
            mConnectTask = null;
        }

        // Cancel any thread currently running a connection
        if (mReceiveTask != null) {
            mReceiveTask.cancel(true);
            mReceiveTask = null;
        }
    }

    protected void onConnectDone(BluetoothSocket socket) {
        setState(STATE_CONNECTED);
        mReceiveTask = new ReceiveTask();
        mReceiveTask.execute(socket);
    }

    protected void onConnectFail() {
        mConnectTask = null;
        setState(STATE_NONE);
        if (mReconnect) {
            doConnect(mDeviceWithConnected, mConnectionSecureType);
        }
    }

    protected void onReceiveFail(Exception e) {
        setState(STATE_NONE);
        if (mReconnect) {
            doConnect(mDeviceWithConnected, mConnectionSecureType);
        }
    }

    protected abstract void onReceivedData(byte[] values);

    /**
     * Stop all threads
     */
    public void disconnect() {
        Log.d(TAG, "disconnect");
        mReconnect = false;

        cancelTasks();

        setState(STATE_NONE);
    }

    private void connectionFailed() {
        if (mReconnect) {
            //Log("Connection failed, try again... ");
            doConnect(mDeviceWithConnected, mConnectionSecureType);
        } else {
            disconnect();
        }
    }

    private void connectionLost() {
        if (mReconnect) {
            doConnect(mDeviceWithConnected, mConnectionSecureType);
        } else {
            disconnect();
        }
    }

    private class ConnectTask extends AsyncTask<BluetoothDevice, Integer, BluetoothSocket> {

        private boolean secure;

        public ConnectTask(boolean secure) {
            this.secure = secure;
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
            String mSocketType;
            BluetoothDevice device = params[0];
            BluetoothSocket socket = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                } else {
                    socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                return null;
            }

            if (isCancelled()) {
                return null;
            }

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                return null;
            }

            // Start the connected thread
            return socket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            super.onPostExecute(bluetoothSocket);
            if (bluetoothSocket != null) {
                onConnectDone(bluetoothSocket);
            } else {
                onConnectFail();
            }

        }
    }

    private class ReceiveTask extends AsyncTask<BluetoothSocket, byte[], Exception> {

        @Override
        protected Exception doInBackground(BluetoothSocket... params) {
            BluetoothSocket socket = params[0];
            InputStream tmpIn;
            //OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                //tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
                return e;
            }

            ByteArrayOutputStream bs = new ByteArrayOutputStream();

            while (!isCancelled()) {
                try {
                    int v = tmpIn.read();

                    if (v == -1) {
                        break;
                    }

                    if (v == '\r' || v == '\n') {
                        byte[] b = bs.toByteArray();
                        if (b.length > 0) {
                            publishProgress(b);
                            bs.reset();
                        }
                    } else {
                        bs.write(v);
                    }
                    /*
                    if (tmpIn.available() > 0) {
                        byte[] bytes = new byte[tmpIn.available()];
                        tmpIn.read(bytes);
                        if (bytes.length <= 4) {
                            Log.d("a", "a");
                        }
                        publishProgress(bytes);
                    } else {
                        int r = tmpIn.read();
                        if (tmpIn.available() > 0) {
                            byte[] bytes = new byte[tmpIn.available() + 1];
                            bytes[0] = (byte) r;
                            tmpIn.read(bytes, 1, bytes.length - 1);
                            if (bytes.length <= 4) {
                                Log.d("a", "a");
                            }
                            publishProgress(bytes);
                        }
                    }*/
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    return e;
                }
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(byte[]... values) {
            super.onProgressUpdate(values);
            onReceivedData(values[0]);
        }

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);
            if (e != null) {
                onReceiveFail(e);
            }
        }
    }
}
