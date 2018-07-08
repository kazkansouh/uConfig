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

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.imaginfire.uconfig.discovery.DiscoveryFragment;
import com.imaginfire.uconfig.editor.EditorFragment;

public class MainActivity extends AppCompatActivity implements DiscoveryFragment.OnDeviceSelectedListener {
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
    }

    @Override
    public void onDeviceSelected(String api, String name) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.fragment_container, EditorFragment.newInstance(api), "edit_fragment");
        t.addToBackStack(null);
        t.commit();
    }
}
