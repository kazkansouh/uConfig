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

package com.imaginfire.uconfig.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DeviceViewModel extends ViewModel implements UDPReceiver.OnEventListener {
    private static final String TAG = "DeviceViewModel";
    private boolean closed = false;
    private final ArrayList<Device> _devices_list = new ArrayList<>();
    private final MutableLiveData<ArrayList<Device>> devices_list = new MutableLiveData<>();
    private final MutableLiveData<Boolean> is_scan_error = new MutableLiveData<>();
    private final UDPReceiver udpserver_thread = new UDPReceiver();
    private final Thread tick_thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!closed && !Thread.interrupted()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Tick thread exit");
                    return;
                }
                synchronized (tick_thread) {
                    if (_devices_list.removeIf(Device::tick)) {
                        notify_update();
                    }
                }
            }
        }
    });

    public DeviceViewModel() {
        devices_list.setValue(new ArrayList<>());
        is_scan_error.setValue(false);
        udpserver_thread.setEventListener(this);
        udpserver_thread.start();
        tick_thread.start();
    }

    public LiveData<? extends List<Device>> getDeviceList() {
        return devices_list;
    }

    private void notify_update() {
        devices_list.postValue(new ArrayList<>(_devices_list));
    }

    @Override
    public void OnReceive(InetAddress address, JSONObject packet) {
        JSONObject beacon = packet.optJSONObject("beacon");
        if (beacon != null) {
            try {
                String api = beacon.getString("api");
                String name = beacon.getString("name");
                String id = beacon.getString("id");
                beacon(address, api, name, id);
            } catch (JSONException e) {
                Log.e(TAG, "invalid beacon received", e);
            }
        }
        JSONObject data = packet.optJSONObject("data");
        if (data != null) {
            try {
                String name = data.getString("name");
                String type = data.getString("type");
                Value v = null;
                if (type.equals("INT")) {
                    v = new Value(null, data.getInt("value"), null);
                }
                if (type.equals("UINT8")) {
                    int i = data.getInt("value");
                    if (i < 0 || i > 0xFF) {
                        Log.w(TAG, "Out of range value for UINT8 received, ignoring.");
                    } else {
                        v = new Value(i, null, null);
                    }
                }
                if (type.equals("STRING")) {
                    v = new Value(null, null, data.getString("value"));
                }

                if (v != null) {
                    update(address, name, v);
                } else {
                    Log.w(TAG, "Unable to translate data beacon, ignoring.");
                }
            } catch (JSONException e) {
                Log.e(TAG, "invalid data beacon received", e);
            }
        }
    }

    private void beacon(InetAddress address, String api, String name, String id) {
        synchronized (tick_thread) {
            for (Device d : _devices_list) {
                if (d.address.equals(address)) {
                    d.touch();
                    return;
                }
            }
            _devices_list.add(new Device(address, api, name + " (" + id + ")"));
            notify_update();
        }
    }

    private void update(InetAddress address, @NonNull String name, @NonNull Value v) {
        synchronized (tick_thread) {
            for (Device d : _devices_list) {
                if (d.address.equals(address)) {
                    d.onVariableUpdate(name, v);
                    return;
                }
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        closed = true;
        udpserver_thread.shutdown();

        tick_thread.interrupt();
    }

    @Override
    public void OnError() {
        is_scan_error.postValue(true);
    }
}
