package com.waveloom.radio;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private BroadcastReceiver audioDeviceReceiver;

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Phone call or another app started playing -> Pause Waveloom
                evalJs("window.onNativeAudioFocus && window.onNativeAudioFocus('pause')");
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // Phone call ended -> Resume Waveloom
                evalJs("window.onNativeAudioFocus && window.onNativeAudioFocus('resume')");
                break;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // JavaScript bridge to update the Home Screen Widget with current station info
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void updateStation(String name, String type) {
                    WaveloomWidget.updateAllWidgets(MainActivity.this, name + " (" + type + ")");
                }
            }, "AndroidWidget");
        }

        // Listen for Bluetooth & Wired Headset Connect / Disconnect
        audioDeviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action) ||
                    BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    // Bluetooth or Headphones disconnected -> Pause immediately
                    evalJs("window.onNativeBluetooth && window.onNativeBluetooth('disconnected')");
                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    // Car Bluetooth or Headset connected -> Auto-Play if enabled
                    evalJs("window.onNativeBluetooth && window.onNativeBluetooth('connected')");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioDeviceReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(audioDeviceReceiver, filter);
        }

        // Handle widget command if app was launched from cold start via a widget button
        handleWidgetIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetIntent(intent);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && WaveloomWidget.ACTION_WIDGET_CMD.equals(intent.getAction())) {
            String cmd = intent.getStringExtra("cmd");
            if (cmd != null) {
                evalJs("window.handleWidgetCommand && window.handleWidgetCommand('" + cmd + "')");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requestAudioFocus();
    }

    private void requestAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void evalJs(String script) {
        runOnUiThread(() -> {
            if (getBridge() != null && getBridge().getWebView() != null) {
                getBridge().getWebView().evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void onDestroy() {
        if (audioDeviceReceiver != null) {
            try {
                unregisterReceiver(audioDeviceReceiver);
            } catch (IllegalArgumentException ignored) {}
        }
        super.onDestroy();
    }
}
