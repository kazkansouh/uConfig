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

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.imaginfire.uconfig.R;

public class RecyclerViewHolder extends RecyclerView.ViewHolder {
    // each data item is just a string in this case
    private ConstraintLayout view;
    RecyclerViewHolder(ConstraintLayout v) {
        super(v);
        view = v;
    }

    public void setDeviceName(String str) {
        TextView v = view.findViewById(R.id.text_device_name);
        v.setText(str);
    }

    public void setLocation(String str) {
        TextView v = view.findViewById(R.id.text_device_location);
        v.setText(str);
    }

    public void setTimeoutMax(int max) {
        ProgressBar p = view.findViewById(R.id.progress_timeout);
        p.setMax(max);
    }

    public void setTimeoutRemain(int x) {
        ProgressBar p = view.findViewById(R.id.progress_timeout);
        p.setProgress(x);
    }
}
