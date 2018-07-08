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

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.Device;
import com.imaginfire.uconfig.model.DeviceViewModel;

class RecyclerAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private static final String TAG = "RecyclerAdaptor";
    private final DeviceViewModel devices_model;
    private final LifecycleOwner owner;
    private OnItemSelectedListener listener;

    RecyclerAdapter(LifecycleOwner o, DeviceViewModel model) {
        devices_model = model;
        owner = o;
        model.getDeviceList().observe(owner, list -> this.notifyDataSetChanged());
    }

    public void setOnItemSelectedListener(OnItemSelectedListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_view, parent, false);
        v.setOnClickListener( view -> {
            if (parent instanceof RecyclerView) {
                onSelected(((RecyclerView) parent).getChildAdapterPosition(v));
            }
        });
        return new RecyclerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull  RecyclerViewHolder holder, int position) {
        if (devices_model.getDeviceList().getValue() != null) {
            Device d = devices_model.getDeviceList().getValue().get(position);
            holder.setDeviceName(d.getName());
            holder.setLocation(d.getApiLocation());
            holder.setTimeoutMax(Device.max_ttl);
            d.getTTL().observe(owner, holder::setTimeoutRemain);
        } else {
            holder.setDeviceName("Error loading device");
            holder.setLocation("");
        }
    }

    @Override
    public int getItemCount() {
        if (devices_model.getDeviceList().getValue() != null) {
            return devices_model.getDeviceList().getValue().size();
        }
        return 0;
    }

    private void onSelected(int i) {
        Log.d(TAG, "selected " + i);
        if (listener != null && devices_model.getDeviceList().getValue() != null) {
            try {
                Device d = devices_model.getDeviceList().getValue().get(i);
                listener.onItemSelected(d);
            } catch (IndexOutOfBoundsException e) {
                // do nothing
            }
        }
    }

    interface OnItemSelectedListener {
        void onItemSelected(Device d);
    }
}
