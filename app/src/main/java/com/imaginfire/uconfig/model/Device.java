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

import java.net.InetAddress;
import java.util.ArrayList;

import static java.lang.Integer.max;

public class Device {
    final InetAddress address;
    public static final int max_ttl = 45*4;
    private final MutableLiveData<Integer> ttl = new MutableLiveData<>();
    private final String name;
    private final String api;
    private final ArrayList<OnVariableBroadcastListener> listeners = new ArrayList<>();

    Device(InetAddress a, String api, String n) {
        address = a;
        this.api = api;
        name = n;
        touch();
    }

    void touch() {
        synchronized (this) {
            ttl.postValue(max_ttl);
        }
    }

    boolean tick() {
        int i;
        synchronized (this) {
            if (ttl.getValue() == null) {
                return true;
            }
            i = max(0,ttl.getValue() - 1);
            ttl.postValue(i);
        }
        return i == 0;
    }

    public LiveData<Integer> getTTL() {
        return ttl;
    }

    public String getName() {
        return name;
    }

    public String getApiLocation() {
        return "http://" + address.getHostAddress() + api;
    }

    @Override
    public String toString() {
        return getName() + " " + address.getHostAddress();
    }

    public void addVariableBroadcastListener(OnVariableBroadcastListener l) {
        listeners.add(l);
    }

    public void removeVariableBroadcastListener(OnVariableBroadcastListener l) {
        listeners.remove(l);
    }

    void onVariableUpdate(String var, Value value) {
        for (OnVariableBroadcastListener l : listeners) {
            l.onVariableBroadcast(var, value);
        }
    }

    interface OnVariableBroadcastListener {
        void onVariableBroadcast(String var, Value value);
    }
}