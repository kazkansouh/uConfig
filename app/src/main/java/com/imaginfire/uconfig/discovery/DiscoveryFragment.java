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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.Device;
import com.imaginfire.uconfig.model.DeviceViewModel;

public class DiscoveryFragment extends Fragment {

    private OnDeviceSelectedListener mListener;
    private DeviceViewModel model = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get reference to view model
        model = ViewModelProviders.of(getActivity()).get(DeviceViewModel.class);
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

    private void onSelect(Device d) {
        if (mListener != null) {
            mListener.onDeviceSelected(d.getApiLocation(), d.getName());
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
    }
}
