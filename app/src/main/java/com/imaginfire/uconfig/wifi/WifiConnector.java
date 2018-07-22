/*
 * Copyright (c) 2018 Karim Kanso. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.imaginfire.uconfig.wifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WifiConnector {
    private static final String TAG = "WifiConnector";

    private final String ssid;
    private final String passphrase;
    private final WifiManager wifimgr;
    private final Context context;
    private final BroadcastReceiver receiver;
    private final BlockingQueue<NetworkInfo.State> states = new LinkedBlockingQueue<>();
    private OnStateChangeListener statechange;
    private OnProgressUpdateListener progress;
    private boolean stopRequested = false;
    private boolean isConnected = false;
    private boolean isStarted = false;

    public WifiConnector(@NonNull String ssid,
                         @NonNull String psk,
                         @NonNull WifiManager mgr,
                         @NonNull Context ctx,
                         OnProgressUpdateListener updateListener,
                         OnStateChangeListener stateChangeListener) {
        this.ssid = ssid;
        passphrase = psk;
        wifimgr = mgr;
        context = ctx;
        statechange = stateChangeListener;
        progress = updateListener;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null && intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo networkInfo =
                            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    Log.d(TAG, "WiFi State: " + networkInfo.getState() + " - " + wifimgr.getConnectionInfo().getSSID());
                    if (states != null) {
                        states.offer(networkInfo.getState());
                    }
                }
            }
        };
    }

    public void start() {
        // protect against starting multiple times
        synchronized (this) {
            if (isStarted) {
                return;
            }
            isStarted = true;
        }
        new Thread(() -> {
            int netid = -1;

            for (WifiConfiguration c : wifimgr.getConfiguredNetworks()) {
                if (c.SSID.equals("\"" + ssid + "\"")) {
                    netid = c.networkId;
                }
            }

            if (netid == -1) {
                Log.i(TAG, "Adding wifi network.");
                WifiConfiguration c = new WifiConfiguration();
                c.SSID = "\"" + ssid + "\"";
                c.preSharedKey = "\"" + passphrase + "\"";
                netid = wifimgr.addNetwork(c);
            }

            wifimgr.disconnect();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted on initial sleep");
                return;
            }

            // start receiving wifi events
            context.registerReceiver(receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
            wifimgr.enableNetwork(netid, true);

            publishProgress("Connecting to " + ssid + "...", true);

            int failCounter = 0;
            do {
                NetworkInfo.State s = NetworkInfo.State.UNKNOWN;
                try {
                    // wait for notifications from networkStateChanged
                    s = states.poll(45, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.i(TAG, "Stopping wifi connector due interruption.");
                    stop();
                }

                if (s == null) {
                    if (!isConnected) {
                        publishProgress("Connect timeout", false);
                        stop();
                    }
                } else {
                    switch (s) {
                        case CONNECTING:
                            break;
                        case CONNECTED:
                            if (ContextCompat.checkSelfPermission(context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                                Log.e(TAG, "Permission not granted.");
                                stop();
                            } else if (wifimgr.getConnectionInfo().getSSID().equals("\"" + ssid + "\"")) {
                                publishProgress("Connected!", true);
                                publishStateChange(State.Connect);
                                failCounter = 0;
                            } else {
                                publishProgress("Connect fail, retrying", true);
                                if (isConnected) {
                                    publishStateChange(State.Disconnect);
                                }
                                wifimgr.enableNetwork(netid, true);
                            }
                            break;
                        case DISCONNECTING:
                        case DISCONNECTED:
                            if (isConnected) {
                                publishStateChange(State.Disconnect);
                                wifimgr.enableNetwork(netid, true);
                                publishProgress("Reconnect", true);
                            } else if (wifimgr.getConnectionInfo().getSSID().equals("\"" + ssid + "\"")) {
                                boolean alive = false;
                                for (ScanResult r : wifimgr.getScanResults()) {
                                    if (r.SSID.equals(ssid)) {
                                        // network is still alive
                                        alive = true;
                                    }
                                }
                                if (!alive) {
                                    failCounter++;
                                    wifimgr.startScan();
                                }
                                if (failCounter >= 3) {
                                    publishProgress("Network gone", false);
                                    stop();
                                }
                            }
                        case SUSPENDED:
                        case UNKNOWN:
                        default:
                    }
                }
            } while (!stopRequested);

            if (isConnected) {
                wifimgr.disconnect();
            }
            wifimgr.disableNetwork(netid);
            wifimgr.removeNetwork(netid);
            wifimgr.reconnect();
            publishProgress("Restored wifi settings", false);

            publishStateChange(ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED ? State.NoPermission : State.Finished);
        }).start();
    }

    private void publishProgress(String msg, boolean abortable) {
        Log.i(TAG, msg);
        OnProgressUpdateListener p = progress;
        if (p != null) {
            p.onProgressUpdate(msg, abortable);
        }
    }

    private void publishStateChange(State state) {
        switch (state) {
            case Connect:
                isConnected = true;
                break;
            case Disconnect:
            case Finished:
                isConnected = false;
        }

        OnStateChangeListener p = statechange;
        if (p != null) {
            p.onStateChange(state);
        }
    }

    public synchronized void stop() {
        Log.d(TAG, "Stop requested");
        if (!stopRequested) {
            stopRequested = true;
            // stop receiving wifi events
            context.unregisterReceiver(receiver);
            states.offer(NetworkInfo.State.UNKNOWN);
        }
    }

    public void setOnProgressUpdateListener(OnProgressUpdateListener p) {
        progress = p;
    }

    public void setOnStateChangeListener(OnStateChangeListener p) {
        statechange = p;
    }

    public enum State {
        Connect, Disconnect, Finished, NoPermission
    }

    public interface OnProgressUpdateListener {
        void onProgressUpdate(String msg, boolean abortable);
    }

    public interface OnStateChangeListener {
        void onStateChange(State s);
    }
}
