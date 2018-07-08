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

package com.imaginfire.uconfig.editor;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.Device;
import com.imaginfire.uconfig.model.DeviceViewModel;
import com.imaginfire.uconfig.model.SchemaViewModel;

public class EditorFragment extends Fragment {
    private static final String TAG = "EditorFragment";
    private static final String ARG_API_LOCATION = "api_location";
    private static final String PARAM_SELECTED_MENU = "com.imaginfire.uconfig.editor.SELECTED_MENU_ITEM_ID";

    private VariableAdapter var_adapter = null;
    private ActionAdapter act_adapter = null;
    private Device device = null;
    private SchemaViewModel schema = null;

    public static EditorFragment newInstance(String api) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_API_LOCATION, api);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get reference to view model
        DeviceViewModel model = ViewModelProviders.of(getActivity()).get(DeviceViewModel.class);
        if (getArguments() != null) {
            String api = getArguments().getString(ARG_API_LOCATION,"");
            if (model.getDeviceList().getValue() != null) {
                for (Device d : model.getDeviceList().getValue()) {
                    if (d.getApiLocation().equals(api)) {
                        device = d;
                        break;
                    }
                }
            }
            if (device == null) {
                Log.e(TAG, "Device not found: " + api);
                getFragmentManager().popBackStack();
                return;
            }
        } else {
            throw new RuntimeException("Missing parameter from fragment initialisation.");
        }
        schema = ViewModelProviders.of(this).get(device.getApiLocation(), SchemaViewModel.class);
    }

    private RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter(int resid) {
        switch (resid) {
            case R.id.action_actions:
                return act_adapter;
            case R.id.action_variables:
                return var_adapter;
        }
        Log.e(TAG, "No corresponding recycler adapter for resource " + resid);
        return null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (device == null) {
            return null;
        }
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_editor, container, false);
        if (!(v instanceof ConstraintLayout)) {
            throw new ClassCastException("fragment_editor.xml does not contain ConstraintLayout at root.");
        }
        ConstraintLayout c = (ConstraintLayout)v;
        ProgressBar ttl = c.findViewById(R.id.progress_timeout);
        ttl.setMax(Device.max_ttl);
        //ttl.setProgress(device.getTTL().getValue());
        device.getTTL().observe(this, i -> {
            if (i == null || i == 0) {
                // device has disappeared from network, close fragment
                getFragmentManager().popBackStack();
                return;
            }
            ttl.setProgress(i);
        });

        var_adapter = new VariableAdapter(this, schema, savedInstanceState);
        act_adapter = new ActionAdapter(this, schema, savedInstanceState);

        v = c.findViewById(R.id.schema_recycler);
        if (v instanceof RecyclerView) {
            RecyclerView r = (RecyclerView) v;
            r.setHasFixedSize(true);
            r.setLayoutManager(new LinearLayoutManager(getActivity()));

            BottomNavigationView t = c.findViewById(R.id.toolbar_schema);
            t.setOnNavigationItemSelectedListener(item -> {
              r.setAdapter(getAdapter(item.getItemId()));
              return true;
            });
            int selected = R.id.action_variables;
            if (savedInstanceState != null) {
                selected = savedInstanceState.getInt(PARAM_SELECTED_MENU, selected);
            }
            t.setSelectedItemId(selected);

        } else {
            throw new ClassCastException("fragment_editor.xml should contain schema_recycler.");
        }

        schema.getVariables().observe(this, vars -> c.findViewById(R.id.progress_loading).setVisibility(View.INVISIBLE));

        // initialise will trigger callbacks via livedata
        schema.initialise(device);
        return c;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        var_adapter.onSaveInstanceState(outState);
        act_adapter.onSaveInstanceState(outState);
        if (getView() != null) {
            BottomNavigationView t = getView().findViewById(R.id.toolbar_schema);
            outState.putInt(PARAM_SELECTED_MENU, t.getSelectedItemId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity a = (AppCompatActivity)getActivity();
            if (a.getSupportActionBar() != null) {
                a.getSupportActionBar().setTitle(device.getName());
            }
        }
    }

}
