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

package com.imaginfire.uconfig;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.imaginfire.uconfig.discovery.DiscoveryFragment;
import com.imaginfire.uconfig.editor.EditorFragment;
import com.imaginfire.uconfig.wifi.WifiConnector;

public class MainActivity extends AppCompatActivity implements DiscoveryFragment.OnDeviceSelectedListener {
    private static final int PERMISSION_LOCATION_REQUEST = 800;
    private static final String TAG = "MainActivity";

    private WifiConnector connector = null;
    private Switch wifi_switch = null;
    private View permission_pane = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            t.add(R.id.fragment_container, new DiscoveryFragment(), "discovery_fragment");
            t.commit();

            onDeviceSelected("", "");
        }

        {
            Button b = findViewById(R.id.button_grant);
            if (b == null) {
                throw new RuntimeException("button_grant Button not found");
            }
            b.setOnClickListener(view -> requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_LOCATION_REQUEST));
        }

        permission_pane = findViewById(R.id.permission_pane);
        if (permission_pane == null) {
            throw new RuntimeException("permission_pane View not found");
        }
        permission_pane.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.wifi_switch_action);
        if (item == null) {
            throw new RuntimeException("Could not find wifi_switch_action menu item.");
        }
        wifi_switch = item.getActionView().findViewById(R.id.switch_wifi);
        if (wifi_switch == null) {
            throw new RuntimeException("Could not find switch_wifi in wifi_switch_action's layout.");
        }
        wifi_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById(R.id.root_pane), "Location permission not granted", Snackbar.LENGTH_LONG).show();
                    buttonView.setChecked(false);
                    return;
                }

                WifiManager wifimgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                if (wifimgr == null || !wifimgr.startScan()) {
                    Snackbar.make(findViewById(R.id.root_pane), "Unable to scan wifi", Snackbar.LENGTH_LONG).show();
                    buttonView.setChecked(false);
                }
            } else {
                synchronized (MainActivity.this) {
                    if (connector != null) {
                        connector.stop();
                    }
                }
            }
        });
        return true;
    }

    @Override
    public void onDeviceSelected(String api, String name) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.fragment_container, EditorFragment.newInstance(api), "edit_fragment");
        t.addToBackStack(null);
        t.commit();
    }

    @Override
    public void onRequestWifi(String ssid, String psk) {
        if (connector == null && wifi_switch != null && wifi_switch.isChecked()) {
            WifiManager wifimgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (connector == null && wifimgr != null) {
                connector = new WifiConnector(ssid, psk, wifimgr, this, (msg, abortable) -> {
                    Snackbar sb = Snackbar.make(findViewById(R.id.root_pane), msg, Snackbar.LENGTH_LONG);
                    if (abortable) {
                        sb.setAction("Abort", v -> {
                            if (wifi_switch != null && wifi_switch.isChecked()) {
                                wifi_switch.setChecked(false);
                            }
                            synchronized (MainActivity.this) {
                                if (connector != null) {
                                    connector.stop();
                                }
                            }
                        });
                    }
                    sb.show();
                }, s -> {
                    switch (s) {
                        case Connect:
                        case Disconnect:
                            break;
                        case NoPermission:
                            onNoPermission();
                            // no break!
                        case Finished:
                            synchronized (MainActivity.this) {
                                connector = null;
                            }
                    }
                });
                connector.start();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission_pane.setVisibility(View.GONE);
                    WifiManager wifimgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    if (wifimgr != null) {
                        wifimgr.startScan();
                    }
                } else {
                    Log.i(TAG, "Permission request denied.");
                    if (permission_pane != null && permission_pane.getVisibility() != View.VISIBLE) {
                        permission_pane.setVisibility(View.VISIBLE);
                    }
                    if (wifi_switch != null && wifi_switch.isChecked()) {
                        wifi_switch.setChecked(false);
                    }
                }
            }
        }
    }

    @Override
    public void onNoPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (permission_pane != null && permission_pane.getVisibility() != View.VISIBLE) {
                permission_pane.setVisibility(View.VISIBLE);
            }
            if (wifi_switch != null && wifi_switch.isChecked()) {
                wifi_switch.setChecked(false);
            }
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_LOCATION_REQUEST);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        synchronized (MainActivity.this) {
            if (connector != null) {
                // disconnect listener as it updates ui
                connector.setOnProgressUpdateListener(null);
                connector.stop();
            }
        }
    }
}
