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

package pl.nkg.brq.android.sensors;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.UnsupportedEncodingException;

import pl.nkg.brq.android.communication.AbstractBluetoothCommunication;

public class DistanceBluetooth extends Distance {

    private static final String TAG = DistanceBluetooth.class.getSimpleName();

    private String mBluetoothDevice;
    //private byte[] buff = new byte[16];
    //private int buffPos = 0;

    private AbstractBluetoothCommunication mBluetoothCommunication = new AbstractBluetoothCommunication() {
        @Override
        protected void onReceivedData(byte[] values) {
            if (!isRunning()) {
                return;
            }

            try {
                double v = Double.parseDouble(new String(values, "UTF-8"));
                if (v > 20) {
                    setValue(v);
                }
                //Log.d(TAG, v + "cm");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid format number. Data from device is corrupted.");
            }


            /*for (byte value : values) {
                if (buffPos >= buff.length) {
                    buffPos = 0;
                    Log.e(TAG, "Buffer for SPP overflow. Data from device is corrupted.");
                }

                if (buffPos > 0 && (value == '\n' || value == '\r')) {
                    try {
                        double v = Double.parseDouble(new String(buff, 0, buffPos, "UTF-8"));
                        if (v > 20) {
                            setValue(v);
                        }

                        if (v < 40) {
                            Log.d("s", "s");
                        }
                        Log.d(TAG, v + "cm");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid format number. Data from device is corrupted.");
                    }
                    buffPos = 0;
                } else if (value != '\n' && value != '\r') {
                    buff[buffPos] = value;
                    buffPos++;
                }
            }*/
        }
    };

    public DistanceBluetooth(String bluetoothDevice) {
        this.mBluetoothDevice = bluetoothDevice;
    }

    @Override
    public void start() throws SecurityException {
        super.start();
        mBluetoothCommunication.connect(mBluetoothDevice, false);
    }

    @Override
    public void stop() {
        mBluetoothCommunication.disconnect();
        super.stop();
    }
}
