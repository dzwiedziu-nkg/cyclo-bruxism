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

public class Value {
    private double mValue;
    private double mMinValue;
    private double mMaxValue;

    private long timestampOfUpdated;
    private long timestampOfFirst;

    private boolean mNextClean = true;


    public synchronized double getMaxValue() {
        return mMaxValue;
    }

    public synchronized double getMinValue() {
        return mMinValue;
    }

    public synchronized double getValue() {
        return mValue;
    }

    public synchronized long getTimestampOfFirst() {
        return timestampOfFirst;
    }

    public synchronized long getTimestampOfUpdated() {
        return timestampOfUpdated;
    }

    public synchronized boolean isNextClean() {
        return mNextClean;
    }

    public synchronized void setNextClean(boolean nextClean) {
        mNextClean = nextClean;
    }

    public synchronized void setValue(double value) {
        timestampOfUpdated = System.currentTimeMillis();
        mValue = value;

        if (mNextClean) {
            timestampOfFirst = timestampOfUpdated;
            mMinValue = mValue;
            mMaxValue = mValue;
        } else  {
            mMinValue = Math.min(mValue, mMinValue);
            mMaxValue = Math.max(mValue, mMaxValue);
        }

        mNextClean = false;
    }
}
