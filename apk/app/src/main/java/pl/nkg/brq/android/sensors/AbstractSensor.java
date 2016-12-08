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


abstract class AbstractSensor {

    private boolean mRunning = false;

    private NewValueAvailableListener mNewValueAvailableListener;

    public boolean isRunning() {
        return mRunning;
    }

    public void start() throws SecurityException {
        mRunning = true;
    }

    public void stop() {
        mRunning = false;
    }

    protected void emitNewValueAvailableListener() {
        if (mNewValueAvailableListener != null && mRunning) {
            mNewValueAvailableListener.onNewValueAvailable();
        }
    }

    public NewValueAvailableListener getNewValueAvailableListener() {
        return mNewValueAvailableListener;
    }

    public void setNewValueAvailableListener(NewValueAvailableListener newValueAvailableListener) {
        mNewValueAvailableListener = newValueAvailableListener;
    }

    interface NewValueAvailableListener {
        void onNewValueAvailable();
    }
}
