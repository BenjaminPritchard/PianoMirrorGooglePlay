/*

Kundalini Piano Mirror, Android (Google Play) Version

Based off Midi Examples from Google

The initial version is very rough, and sort of hacked together!!!
But it does implement the basic functionality

TODO:
    - clean up this code!!!!

History
-------

1.0     4-April-2019:       Initial Release; based off example from Google
1.1     16-March-2020:      Updated targetSdkVersion to 28

 */

package com.kundalinisoftware.PianoMirrorGooglePlay;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kundalinisoftware.PianoMirrorGooglePlay.miditools.MidiInputPortSelector;
import com.kundalinisoftware.PianoMirrorGooglePlay.miditools.MidiOutputPortSelector;

import java.io.IOException;
import java.util.LinkedList;

/*
 * Print incoming MIDI messages to the screen.
 */
public class MainActivity extends Activity implements ScopeLogger {
    private static final String TAG = "MidiScope";

    private MidiInputPortSelector mKeyboardReceiverSelector;

    private LinkedList<String> logLines = new LinkedList<String>();
    private static final int MAX_LINES = 100;
    private MidiOutputPortSelector mLogSenderSelector;
    private MidiManager mMidiManager;
    private LoggingReceiver mLoggingReceiver;
    private MidiFramer mConnectFramer;
    private MyDirectReceiver mDirectReceiver;
    private boolean mShowRaw;
    private byte[] mByteBuffer = new byte[3];
    private byte CurMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            // everything is
        } else {
            Toast.makeText(this, "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }

        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        // Receiver that prints the messages.
        mLoggingReceiver = new LoggingReceiver(this);

        // Receivers that parses raw data into complete messages.
        mConnectFramer = new MidiFramer(mLoggingReceiver);

        // Setup a menu to select an input source.
        mLogSenderSelector = new MidiOutputPortSelector(mMidiManager, this,
                R.id.spinner_senders) {

            @Override
            public void onPortSelected(final MidiPortWrapper wrapper) {
                super.onPortSelected(wrapper);
                if (wrapper != null) {
                    log(MidiPrinter.formatDeviceInfo(wrapper.getDeviceInfo()));
                }
            }
        };

        mKeyboardReceiverSelector = new MidiInputPortSelector(mMidiManager,
                this, R.id.spinner_receivers);

        mDirectReceiver = new MyDirectReceiver();
        mLogSenderSelector.getSender().connect(mDirectReceiver);

        mLoggingReceiver.setSender(mKeyboardReceiverSelector);

        // Tell the virtual device to log its messages here..
        MidiScope.setScopeLogger(this);

        highlightButton(R.id.button_none);
        normalButton(R.id.left_ascending);
        normalButton(R.id.right_descending);
        normalButton(R.id.mirror);
    }

    @Override
    public void onDestroy() {
        mLogSenderSelector.onClose();
        // The scope will live on as a service so we need to tell it to stop
        // writing log messages to this Activity.
        MidiScope.setScopeLogger(null);
        super.onDestroy();
    }

    private void normalButton(int index) {
        Button btn;

        btn = (Button) findViewById(index);
        btn.setBackgroundColor(Color.LTGRAY);
        btn.setTextColor(Color.BLACK);
    }

    private void highlightButton(int index) {
        Button btn;

        btn = (Button) findViewById(index);
        btn.setBackgroundColor(Color.GREEN);
        btn.setTextColor(Color.BLACK);
    }



    public void onNoTranspose(View view) {

        highlightButton(R.id.button_none);
        normalButton(R.id.left_ascending);
        normalButton(R.id.right_descending);
        normalButton(R.id.mirror);

        mLoggingReceiver.setCurMode(0);
    }

    public void onLeftAscending(View view) {

        normalButton(R.id.button_none);
        highlightButton(R.id.left_ascending);
        normalButton(R.id.right_descending);
        normalButton(R.id.mirror);

        mLoggingReceiver.setCurMode(1);
    }

    public void onRightDescending(View view) {

        normalButton(R.id.button_none);
        normalButton(R.id.left_ascending);
        highlightButton(R.id.right_descending);
        normalButton(R.id.mirror);

        mLoggingReceiver.setCurMode(2);
    }

    public void onMirror (View view) {

        normalButton(R.id.button_none);
        normalButton(R.id.left_ascending);
        normalButton(R.id.right_descending);
        highlightButton(R.id.mirror);

        mLoggingReceiver.setCurMode(3);
    }

    class MyDirectReceiver extends MidiReceiver {
        @Override
        public void onSend(byte[] data, int offset, int count,
                long timestamp) throws IOException {
            if (mShowRaw) {
                String prefix = String.format("0x%08X, ", timestamp);
                logByteArray(prefix, data, offset, count);
            }
            // Send raw data to be parsed into discrete messages.
            mConnectFramer.send(data, offset, count, timestamp);


        }
    }

    /**
     * @param string
     */
    @Override
    public void log(final String string) {
        runOnUiThread(new Runnable() {
                @Override
            public void run() {
                logFromUiThread(string);
            }
        });
    }

    // Log a message to our TextView.
    // Must run on UI thread.
    private void logFromUiThread(String s) {

    }

    private void logByteArray(String prefix, byte[] value, int offset, int count) {
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = 0; i < count; i++) {
            builder.append(String.format("0x%02X", value[offset + i]));
            if (i != count - 1) {
                builder.append(", ");
            }
        }
        log(builder.toString());
    }
}
