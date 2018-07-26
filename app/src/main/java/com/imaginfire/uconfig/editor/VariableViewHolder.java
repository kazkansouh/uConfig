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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.editor.controls.EditValue;
import com.imaginfire.uconfig.model.Value;

public class VariableViewHolder extends BaseViewHolder {
    private static final int S_LOADING = 0x01;
    private static final int S_EXPANDED = 0x02;
    private static final int S_EDITABLE = 0x04;

    private final ConstraintLayout container;
    @NonNull
    private final TextView var_name;
    @NonNull
    private final TextView var_value;
    @NonNull
    private final ProgressBar loading;
    @NonNull
    private final ConstraintLayout expaned_container;
    @NonNull
    private final Button save;
    @NonNull
    private final EditValue edit_value;

    private final int backgroundcolor;
    private final int selectedbackgroundcolor;
    private int state = 0;

    VariableViewHolder(LifecycleOwner owner, ConstraintLayout v) {
        super(owner, v);
        container = v;

        View u = v.findViewById(R.id.text_variable_name);
        if (u != null && u instanceof TextView) {
            var_name = (TextView)u;
        } else {
            throw new RuntimeException("text_variable_name TextView not found");
        }

        u = v.findViewById(R.id.text_variable_value);
        if (u != null && u instanceof TextView) {
            var_value = (TextView)u;
        } else {
            throw new RuntimeException("text_variable_value TextView not found");
        }

        u = v.findViewById(R.id.progress_loading);
        if (u != null && u instanceof ProgressBar) {
            loading = (ProgressBar) u;
        } else {
            throw new RuntimeException("progress_loading ProgressBar not found");
        }

        u = v.findViewById(R.id.edit_pane);
        if (u != null && u instanceof ConstraintLayout) {
            expaned_container = (ConstraintLayout) u;
        } else {
            throw new RuntimeException("edit_pane ConstraintLayout not found");
        }

        u = v.findViewById(R.id.button_set_variable);
        if (u != null && u instanceof Button) {
            save = (Button) u;
        } else {
            throw new RuntimeException("button_set_variable Button not found");
        }

        u = v.findViewById(R.id.editvalue);
        if (u != null && u instanceof EditValue) {
            edit_value = (EditValue) u;
        } else {
            throw new RuntimeException("edit_variable EditValue not found");
        }

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = v.getContext().getTheme();
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        backgroundcolor = typedValue.data;
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        selectedbackgroundcolor = typedValue.data;
    }

    void setVariableName(String s) {
        var_name.setText(s);
    }

    void setValue(Value v) {
        if (v == null) {
            var_value.setText("N/A");
            edit_value.setText("N/A");
        } else {
            var_value.setText(v.getStringValue());
            edit_value.setText(v);
        }
    }

    public Value getValue() {
        return edit_value.getValue();
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

    void setEditable(Value.Type type, boolean editable, View.OnClickListener listener) {
        save.setOnClickListener(listener);
        edit_value.setType(type);
        if (editable) {
            state = state | S_EDITABLE;
        } else {
            state = state & ~S_EDITABLE;
        }
        displayState();
    }

    void clearState() {
        state = 0;
        displayState();
        edit_value.setType(Value.Type.String);
    }

    private void displayState() {
        if ((state & S_EXPANDED) > 0) {
            container.setBackgroundColor(selectedbackgroundcolor);
            expaned_container.setVisibility(View.VISIBLE);
            var_value.setVisibility(View.INVISIBLE);
            if ((state & S_LOADING) > 0) {
                loading.setVisibility(View.VISIBLE);
                save.setVisibility(View.INVISIBLE);
            } else {
                loading.setVisibility(View.INVISIBLE);
                if ((state & S_EDITABLE) > 0) {
                    save.setVisibility(View.VISIBLE);
                    edit_value.setEnabled(true);
                    save.setEnabled(true);
                } else {
                    save.setVisibility(View.INVISIBLE);
                    edit_value.setEnabled(false);
                    save.setEnabled(false);
                }
            }
        } else {
            container.setBackgroundColor(backgroundcolor);
            expaned_container.setVisibility(View.GONE);
            save.setVisibility(View.INVISIBLE);
            if ((state & S_LOADING) > 0) {
                loading.setVisibility(View.VISIBLE);
                var_value.setVisibility(View.INVISIBLE);
            } else {
                loading.setVisibility(View.INVISIBLE);
                var_value.setVisibility(View.VISIBLE);
            }
        }
    }
}
