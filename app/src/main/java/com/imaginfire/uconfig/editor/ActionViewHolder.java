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

import android.arch.lifecycle.LifecycleOwner;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.editor.controls.EditValue;
import com.imaginfire.uconfig.model.Value;

import java.util.HashMap;
import java.util.Map;

public class ActionViewHolder extends BaseViewHolder {
    private static final int S_LOADING = 0x01;
    private static final int S_EXPANDED = 0x02;

    @NonNull
    private final ConstraintLayout container;
    @NonNull
    private final TextView action_name;
    @NonNull
    private final LinearLayout param_pane;
    @NonNull
    private final Button invoke;
    @NonNull
    private final ProgressBar loading;

    private final int backgroundcolor;
    private final int selectedbackgroundcolor;
    private int state = 0;

    ActionViewHolder(LifecycleOwner owner, @NonNull ConstraintLayout c) {
        super(owner, c);

        container = c;
        View u = c.findViewById(R.id.text_action_name);
        if (u != null && u instanceof TextView) {
            action_name = (TextView)u;
        } else {
            throw new RuntimeException("text_action_name TextView not found");
        }

        u = c.findViewById(R.id.parameter_pane);
        if (u != null && u instanceof LinearLayout) {
            param_pane = (LinearLayout)u;
        } else {
            throw new RuntimeException("parameter_pane LinearLayout not found");
        }

        u = c.findViewById(R.id.button_invoke_action);
        if (u != null && u instanceof Button) {
            invoke = (Button)u;
        } else {
            throw new RuntimeException("button_invoke_action Button not found");
        }

        u = c.findViewById(R.id.progress_loading);
        if (u != null && u instanceof ProgressBar) {
            loading = (ProgressBar)u;
        } else {
            throw new RuntimeException("button_invoke_action Button not found");
        }

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = c.getContext().getTheme();
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        backgroundcolor = typedValue.data;
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        selectedbackgroundcolor = typedValue.data;
    }

    void setActionName(String s) {
        action_name.setText(s);
    }

    void setParameters(Map<String, Value.Type> parameters) {
        int index = 0;
        for (Map.Entry<String, Value.Type> e : parameters.entrySet()) {
            View v = param_pane.getChildAt(index);
            if (v instanceof ConstraintLayout) {
                TextView t = v.findViewById(R.id.text_param_name);
                t.setText(e.getKey());
                EditValue f = v.findViewById(R.id.editvalue);
                f.setType(e.getValue());
                f.setText("");
            } else {
                throw new RuntimeException("ConstraintLayout child of param_pane not found");
            }
            index++;
        }
    }

    void setLoading(boolean loading) {
        if (loading) {
            state = state | S_LOADING;
        } else {
            state = state & ~S_LOADING;
        }
        displayState();
    }

    void setExpanded(boolean expanded) {
        if (expanded) {
            state = state | S_EXPANDED;
        } else {
            state = state & ~S_EXPANDED;
        }
        displayState();
    }

    private void displayState() {
        if ((state & S_EXPANDED) > 0) {
            param_pane.setVisibility(View.VISIBLE);
            if ((state & S_LOADING) > 0) {
                loading.setVisibility(View.VISIBLE);
                invoke.setVisibility(View.INVISIBLE);
            } else {
                loading.setVisibility(View.INVISIBLE);
                invoke.setVisibility(View.VISIBLE);
            }
            container.setBackgroundColor(selectedbackgroundcolor);
        } else {
            param_pane.setVisibility(View.GONE);
            invoke.setVisibility(View.INVISIBLE);
            if ((state & S_LOADING) > 0) {
                loading.setVisibility(View.VISIBLE);
            } else {
                loading.setVisibility(View.INVISIBLE);
            }
            container.setBackgroundColor(backgroundcolor);
        }
    }

    void setOnClickListener(View.OnClickListener l) {
        invoke.setOnClickListener(l);
    }

    Map<String,Value> getParameters() {
        HashMap<String,Value> params = new HashMap<>();

        for (int i = 0; i < getItemViewType(); i++) {
            View v = param_pane.getChildAt(i);
            if (v instanceof ConstraintLayout) {
                TextView t = v.findViewById(R.id.text_param_name);
                EditValue e = v.findViewById(R.id.editvalue);
                params.put(t.getText().toString(), e.getValue());
            } else {
                throw new RuntimeException("ConstraintLayout child of param_pane not found");
            }
        }
        return params;
    }
}
