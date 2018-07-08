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
import android.util.Log;

import com.imaginfire.uconfig.http.Request;
import com.imaginfire.uconfig.http.RequestArguments;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SchemaViewModel extends ViewModel implements Device.OnVariableBroadcastListener {
    private static final String TAG = "SchemaViewModel";
    private int initialised = 0;
    private final ArrayList<Variable> _variables = new ArrayList<>();
    private final ArrayList<Action> _actions = new ArrayList<>();
    private Request task = null;
    private Device device = null;

    private final MutableLiveData<List<Variable>> variables = new MutableLiveData<>();
    private final MutableLiveData<List<Action>> actions = new MutableLiveData<>();

    public void initialise(Device d) {
        if (device != null && device != d) {
            throw new RuntimeException("SchemaDataModel initialised with different devices.");
        }
        if (initialised == 0) {
            initialised = 1;
            device = d;
            device.addVariableBroadcastListener(this);
            task = new Request();
            task.execute(new RequestArguments(null, d.getApiLocation() + "schema", jsonObject -> {
                if (jsonObject == null) {
                    return;
                }
                if (jsonObject.has("DATA")) {
                    try {
                        JSONObject data = jsonObject.getJSONObject("DATA");
                        for (Iterator<String> k = data.keys(); k.hasNext(); ) {
                            String name = k.next();
                            JSONObject variable = data.optJSONObject(name);
                            if (variable != null) {
                                boolean read = variable.optBoolean("READ", false);
                                boolean write = variable.optBoolean("WRITE", false);
                                String type = variable.getString("TYPE");

                                Log.d(TAG, name + ":" + type + "(" + read + "|" + write + ")");

                                if (type.equals("UINT8")) {
                                    _variables.add(new Variable(device.getApiLocation(), name, Value.Type.Byte, read, write));
                                }
                                if (type.equals("INT")) {
                                    _variables.add(new Variable(device.getApiLocation(), name, Value.Type.Int, read, write));
                                }
                                if (type.equals("STRING")) {
                                    _variables.add(new Variable(device.getApiLocation(), name, Value.Type.String, read, write));
                                }
                            }
                        }
                        JSONObject actions = jsonObject.getJSONObject("ACTION");
                        for (Iterator<String> k = actions.keys(); k.hasNext(); ) {
                            String name = k.next();
                            JSONObject action = actions.optJSONObject(name);
                            if (action != null) {
                                Action act = new Action(device.getApiLocation(), name);
                                for (Iterator<String> j = action.keys(); j.hasNext(); ) {
                                    String param_name = j.next();
                                    String param_type = action.getString(param_name);
                                    if (param_type.equals("UINT8")) {
                                        act.addParameter(param_name, Value.Type.Byte);
                                    }
                                    if (param_type.equals("INT")) {
                                        act.addParameter(param_name, Value.Type.Int);
                                    }
                                    if (param_type.equals("STRING")) {
                                        act.addParameter(param_name, Value.Type.String);
                                    }
                                }
                                _actions.add(act);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Missing data items from JSON", e);
                    }
                }
                initialised = 2;
                variables.postValue(_variables);
                actions.postValue(_actions);
            }));
        }
        if (initialised == 2) {
            variables.postValue(_variables);
        }
    }

    public LiveData<List<Variable>> getVariables() {
        return variables;
    }

    public LiveData<List<Action>> getActions() {
        return actions;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!task.isCancelled()) {
            task.cancel(true);
        }

        if (initialised > 0) {
            device.removeVariableBroadcastListener(this);
        }
    }

    @Override
    public void onVariableBroadcast(String var, Value value) {
        if (initialised == 2) {
            Log.d(TAG, "Variable broadcast received for: " + var);
            for (Variable v : _variables) {
                if (v.getName().equals(var)) {
                    if (v.getType().equals(value.type)) {
                        v.publishNewValue(value);
                    } else {
                        Log.w(TAG, "Variable broadcast with wrong type received for: " + var);
                    }
                    return;
                }
            }
            Log.w(TAG, "Variable broadcast for unknown variable received: " + var);
        }
    }
}
