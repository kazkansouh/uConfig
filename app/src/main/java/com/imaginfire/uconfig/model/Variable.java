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
import android.util.Log;

import com.imaginfire.uconfig.http.Request;
import com.imaginfire.uconfig.http.RequestArguments;

import org.json.JSONException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Variable {
    private final static String TAG = "Variable";
    private final String name;
    private final boolean read;
    private final boolean write;
    private final MutableLiveData<Value> value = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>();
    private final Value.Type type;
    private final String api;

    Variable(String a, String n, Value.Type t, boolean r, boolean w) {
        api = a;
        name = n;
        read = r;
        write = w;
        type = t;
        busy.setValue(false);
    }

    public String getName() {
        return name;
    }

    public LiveData<Value> getValue() {
        return value;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public boolean isReadable() {
        return read;
    }

    public boolean isWritable() {
        return write;
    }

    void publishNewValue(Value v) {
        if (v.type == type || !isReadable()) {
            value.postValue(v);
            return;
        }
        throw new RuntimeException("Attempt to publish variable of wrong type or access.");
    }

    public Value.Type getType() {
        return type;
    }

    public void startRead(OnAction fail) {
        startRead(false, fail);
    }

    private void startRead(boolean ignorebusy, OnAction fail) {
        if (!isReadable()) {
            value.postValue(null);
        }
        if (busy.getValue() == null || !busy.getValue() || ignorebusy) {
            busy.postValue(true);
            Map<String, String> params = Collections.singletonMap("var", name);
            new Request().execute(
                    new RequestArguments(
                            params,
                            api + "get",
                            v -> {
                                if (v == null) {
                                    busy.postValue(false);
                                    if (fail != null) {
                                        fail.onAction();
                                    }
                                    return;
                                }
                                try {
                                    switch (type) {
                                        case Int:
                                            value.postValue(new Value(null, v.getInt(name), null));
                                            break;
                                        case Byte:
                                            int i = v.getInt(name);
                                            if (i >= 0 && i <= 255) {
                                                value.postValue(new Value(v.getInt(name), null, null));
                                            } else {
                                                Log.e(TAG, "uint8 value read out of bounds: " + name + "=" + i);
                                                value.postValue(null);
                                            }
                                            break;
                                        case String:
                                            value.postValue(new Value(null, null, v.getString(name)));
                                            break;
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Unable to read variable value from json response.", e);
                                    value.postValue(null);
                                    if (fail != null) {
                                        fail.onAction();
                                    }
                                }
                                busy.postValue(false);
                            }));
        } else {
            Log.w(TAG, "Ignoring read variable request as busy.");
        }
    }

    public boolean startWrite(Value v, OnAction ok, OnAction fail) {
        if (!isWritable()) {
            Log.w(TAG, "Attempt to write not writable variable ignored.");
            return false;
        }
        if (v == null || !type.equals(v.type)) {
            Log.w(TAG, "Attempt to write value of wrong type ignored.");
            return false;
        }
        if (busy.getValue() == null || !busy.getValue()) {
            busy.postValue(true);
            HashMap<String, String> params = new HashMap<>();
            params.put("var", name);
            params.put("val", v.toString());
            new Request().execute(new RequestArguments(
                    params,
                    api + "set",
                    obj -> {
                        if (obj == null) {
                            Log.d(TAG, "failed to write value");
                            busy.postValue(false);
                            if (fail != null) {
                                fail.onAction();
                            }
                            return;
                        }
                        String result;
                        try {
                            result = obj.getString("result");
                        } catch (JSONException e) {
                            Log.e(TAG, "Unable to read variable value from json response.");
                            if (fail != null) {
                                fail.onAction();
                            }
                            return;
                        } finally {
                            busy.postValue(false);
                        }
                        if (result.equals("ok")) {
                            if (ok != null) {
                                ok.onAction();
                            }
                            if (isReadable()) {
                                // necessary to set ignorebusy as the posted value of WriteOk will
                                // be processed by main thread so might not be visible in startRead.
                                startRead(true, null);
                            }
                        } else {
                            Log.e(TAG, "Error returned from server while writing variable:" + result);
                            if (fail != null) {
                                fail.onAction();
                            }
                        }
                    }
            ));
        }
        return true;
    }
}
