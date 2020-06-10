/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kundalinisoftware.PianoMirrorGooglePlay;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.kundalinisoftware.PianoMirrorGooglePlay.miditools.MidiInputPortSelector;

import java.io.IOException;

/**
 * Convert incoming MIDI messages to a string and write them to a ScopeLogger.
 * Assume that messages have been aligned using a MidiFramer.
 */
public class LoggingReceiver extends MidiReceiver {
    public static final String TAG = "MidiScope";
    private static final long NANOS_PER_MILLISECOND = 1000000L;
    private static final long NANOS_PER_SECOND = NANOS_PER_MILLISECOND * 1000L;
    private long mStartTime;
    private ScopeLogger mLogger;
    private long mLastTimeStamp = 0;
    private MidiInputPortSelector mKeyboardReceiverSelector;
    private int mCurMode;

    public LoggingReceiver(ScopeLogger logger) {
        mStartTime = System.nanoTime();
        mLogger = logger;
    }

    public void setSender(MidiInputPortSelector KeyboardReceiverSelector) {
        mKeyboardReceiverSelector = KeyboardReceiverSelector;
    }

    public void setCurMode(int theMode) {
        mCurMode = theMode;
    }

    private byte transform(byte note) {

        int retval = note & 0xFF;;
        int offset;

        switch(mCurMode) {

            case 0:
                // do nothing
                break;

            case 1:
                if (note < (byte)62) {
                    offset = 62 - note;
                    retval = 62 + offset;
                } // else do nothing;
                break;

            case 2:
                if (note > 62) {
                    offset = (note - 62);
                    retval = 62 - offset;
                } // else do nothing;
                break;

            case 3:
                if (note == 62) {
                    // do nothing
                }
                else if (note < 62) {
                    offset = (62 - note);
                    retval = 62 + offset;
                }
                else if (note > 62) {
                    offset = (note - 62);
                    retval = 62 - offset;
                }
                break;
        }

        return (byte)retval;
    }

    /*
     * @see android.media.midi.MidiReceiver#onSend(byte[], int, int, long)
     */
    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {

        // don't log or do any transposing with system messages...
        byte statusByte = data[offset];
        int status = statusByte & 0xFF;
        if (status > 240)  return;

        StringBuilder sb = new StringBuilder();
        if (timestamp == 0) {
            sb.append(String.format("-----0----: "));
        } else {
            long monoTime = timestamp - mStartTime;
            long delayTimeNanos = timestamp - System.nanoTime();
            int delayTimeMillis = (int)(delayTimeNanos / NANOS_PER_MILLISECOND);
            double seconds = (double) monoTime / NANOS_PER_SECOND;
            // Mark timestamps that are out of order.
            sb.append((timestamp < mLastTimeStamp) ? "*" : " ");
            mLastTimeStamp = timestamp;
            sb.append(String.format("%10.3f (%2d): ", seconds, delayTimeMillis));
        }
        sb.append(MidiPrinter.formatBytes(data, offset, count));
        sb.append(": ");
        sb.append(MidiPrinter.formatMessage(data, offset, count));
        String text = sb.toString();
        mLogger.log(text);
        Log.i(TAG, text);

        // echo out the data to midi out...
        if (mKeyboardReceiverSelector != null) {
            try {
                // send event immediately
                MidiReceiver receiver = mKeyboardReceiverSelector.getReceiver();
                if (receiver != null) {
                    data[1] = transform(data[1]);
                    receiver.send(data, offset, count, timestamp);
                }
            } catch (IOException e) {
                Log.e(TAG, "mKeyboardReceiverSelector.send() failed " + e);
            }
        }

    }

}
