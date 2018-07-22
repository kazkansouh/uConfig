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

package com.imaginfire.uconfig.discovery;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.Device;
import com.imaginfire.uconfig.model.DeviceViewModel;

import java.util.List;

public class DiscoveryFragment extends Fragment {
    private static final String TAG = "DiscoveryFragment";
    private static final String UCONFIG_WIFI = "uConfig";

    private OnDeviceSelectedListener mListener;
    private DeviceViewModel model = null;
    private BroadcastReceiver receiver = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get reference to view model
        FragmentActivity activity = getActivity();
        if (activity != null) {
            model = ViewModelProviders.of(activity).get(DeviceViewModel.class);
        } else {
            throw new RuntimeException("Fragment must be connected to activity.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_discovery, container, false);
        if (v instanceof RecyclerView) {
            RecyclerView r = (RecyclerView) v;
            r.setHasFixedSize(true);
            r.setLayoutManager(new LinearLayoutManager(getActivity()));
            RecyclerAdapter adaptor = new RecyclerAdapter(this, model);
            adaptor.setOnItemSelectedListener(this::onSelect);
            r.setAdapter(adaptor);
            return r;
        }
        throw new ClassCastException("fragment_discovery.xml should be a recycler.");
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getView() == null) {
            return;
        }
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() == null || getContext() == null) {
                        return;
                    }
                    if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                        if (ContextCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            Log.w(TAG, "Location permission not granted, ignoring intent: " + intent.getAction());
                            onNoPermission();
                            return;
                        }
                        WifiManager wifimgr = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifimgr != null) {
                            List<ScanResult> scanResults = wifimgr.getScanResults();

                            for (ScanResult r : scanResults) {
                                if (r.SSID.startsWith(UCONFIG_WIFI)) {
                                    WifiInfo info = wifimgr.getConnectionInfo();
                                    switch (info.getSupplicantState()) {
                                        case AUTHENTICATING:
                                        case ASSOCIATING:
                                        case ASSOCIATED:
                                        case FOUR_WAY_HANDSHAKE:
                                        case GROUP_HANDSHAKE:
                                        case COMPLETED:
                                            if (info.getSSID().startsWith("\"" + UCONFIG_WIFI)) {
                                                Log.d(TAG, "Already connected to uConfig network, ignoring scan result.");
                                                break;
                                            }
                                        default:
                                            // try connecting to network
                                            Log.i(TAG, "Found uConfig network: " + r.SSID);
                                            if (r.SSID.startsWith(UCONFIG_WIFI + " ")) {
                                                String networkid = r.SSID.substring(UCONFIG_WIFI.length() + 1);
                                                String bytes[] = r.BSSID.toLowerCase().split(":");

                                                if (networkid.length() == 4 && bytes.length == 6) {
                                                    onConnectWifi(r.SSID, networkid + bytes[4] + bytes[5]);
                                                } else {
                                                    onConnectWifi(r.SSID, "nopassword");
                                                }
                                            }

                                    }
                                }
                            }
                        }
                    }
                }
            };
        }

        if (getActivity() != null) {
            getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        } else {
            Log.e(TAG, "null activity in onStart");
        }

        if (getContext() != null) {
            WifiManager wifimgr = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
            if (wifimgr != null) {
                wifimgr.startScan();
            } else {
                Log.e(TAG, "Unable to initiate wifi scan");
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        } else {
            Log.e(TAG, "null activity in onStop");
        }
    }

    private void onSelect(Device d) {
        if (mListener != null) {
            mListener.onDeviceSelected(d.getApiLocation(), d.getName());
        }
    }

    private void onConnectWifi(String ssid, String psk) {
        if (mListener != null) {
            mListener.onRequestWifi(ssid, psk);
        }
    }

    private void onNoPermission() {
        if (mListener != null) {
            mListener.onNoPermission();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDeviceSelectedListener) {
            mListener = (OnDeviceSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity a = (AppCompatActivity)getActivity();
            if (a.getSupportActionBar() != null) {
                a.getSupportActionBar().setTitle(R.string.title_activity_discovery);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(String api, String name);
        void onRequestWifi(String ssid, String psk);
        void onNoPermission();
    }
}
