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
import android.support.annotation.NonNull;
import android.util.Log;

import com.imaginfire.uconfig.http.Request;
import com.imaginfire.uconfig.http.RequestArguments;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class Action {
    private static final String TAG = "Action";
    private final String name;
    private final HashMap<String, Value.Type> params = new HashMap<>();
    private final String api;
    private String display = "";
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>();

    Action(@NonNull String api, @NonNull String name) {
        this.name = name;
        this.api = api;
        busy.setValue(false);
        updateDisplayString();
    }

    void addParameter(String name, Value.Type type) {
        params.put(name, type);
        updateDisplayString();
    }

    public Map<String, Value.Type> getParameterSpecification() {
        return params;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public boolean invoke(Map<String, Value> parameters, OnAction ok, OnAction fail) {
        Log.d(TAG, "Invoking action");

        if (parameters.size() != params.size()) {
            Log.w(TAG, "Inconsistent number of parameters while invoking action, ignoring.");
            return false;
        }

        HashMap<String, String> http_params = new HashMap<>();
        http_params.put("method", name);
        for (Map.Entry<String, Value> e : parameters.entrySet()) {
            if (params.containsKey(e.getKey())) {
                if (e.getValue() == null) {
                    Log.w(TAG, "Null value provided for parameter " + e.getKey() + ", ignoring.");
                    return false;
                }
                if (params.get(e.getKey()) != e.getValue().type) {
                    Log.w(TAG, "Inconsistent types of parameter " + e.getKey() + ", ignoring.");
                    return false;
                }
                http_params.put(e.getKey(), e.getValue().toString());
            } else {
                Log.w(TAG, "Inconsistent names of parameters while invoking, ignoring.");
                return false;
            }
        }

        if (busy.getValue() != null && busy.getValue()) {
            return true;
        }
        busy.postValue(true);

        new Request().execute(new RequestArguments(
                http_params,
                api + "invoke",
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
                    if (!result.equals("ok")) {
                        if (fail != null) {
                            fail.onAction();
                        }
                        Log.e(TAG, "Error returned from server while invoking action:" + result);
                    } else {
                        if (ok != null) {
                            ok.onAction();
                        }
                    }
                }
        ));
        return true;
    }

    public String getName() {
        return name;
    }

    private void updateDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append('(');

        for (Map.Entry<String, Value.Type> e : params.entrySet()) {
            if (sb.charAt(sb.length() - 1) != '(') {
                sb.append(',');
            }
            sb.append(e.getKey());
        }

        sb.append(')');
        display = sb.toString();
    }

    @Override
    public String toString() {
        return display;
    }
}
