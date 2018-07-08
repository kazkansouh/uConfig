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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.SchemaViewModel;
import com.imaginfire.uconfig.model.Value;
import com.imaginfire.uconfig.model.Variable;

class VariableAdapter extends RecyclerView.Adapter<VariableViewHolder> {
    private static final String TAG = "VariableAdaptor";
    private static final String PARAM_EXPANDED = "com.imaginfire.uconfig.editor.VariableAdapter.EXPANDED";

    private final SchemaViewModel schema_model;
    private final LifecycleOwner owner;
    private int expand = -1;

    VariableAdapter(LifecycleOwner o, SchemaViewModel model, Bundle savedInstanceState) {
        schema_model = model;
        owner = o;
        schema_model.getVariables().observe(owner, list -> this.notifyDataSetChanged());

        if (savedInstanceState != null) {
            expand = savedInstanceState.getInt(PARAM_EXPANDED, -1);
        }
    }

    @NonNull
    @Override
    public VariableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.variable_view, parent, false);
        VariableViewHolder holder = new VariableViewHolder(owner, v);
        v.setOnClickListener( view -> {
            if (parent instanceof RecyclerView) {
                if (schema_model.getVariables().getValue() != null) {
                    onSelected(holder);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VariableViewHolder holder, int position) {
        if (schema_model.getVariables().getValue() != null) {
            holder.resetObservers();
            holder.clearState();
            Variable v = schema_model.getVariables().getValue().get(position);
            holder.setVariableName(v.getName());
            v.getValue().observe(holder, value -> {
                    holder.setLoading(false);
                    if (value != null) {
                        holder.setVariableValue(value.getStringValue());
                    } else {
                        holder.setVariableValue(null);
                    }
                });
            if (v.getValue().getValue() == null && v.isReadable()) {
                holder.setLoading(true);
                v.startRead();
            } else {
                holder.setLoading(false);
                // v.getValue().getValue() != null added to pass code analysis, despite
                // being implied by validity of v.isReadable()
                if (v.getValue().getValue() != null && v.isReadable()) {
                    holder.setVariableValue(v.getValue().getValue().getStringValue());
                } else {
                    holder.setVariableValue(null);
                }
            }
            holder.setExpanded(position == expand);
            holder.setEditable(v.getType(), v.isWritable(), btn -> {
                Value value = holder.getValue();
                Log.d(TAG, "Setting to text: " + value);
                if (v.startWrite(value)) {
                    notifyItemChanged(expand);
                    expand = -1;
                    Snackbar.make(holder.itemView, R.string.snack_write_ok, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(holder.itemView, R.string.snack_write_fail, Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (schema_model.getVariables().getValue() != null) {
            return schema_model.getVariables().getValue().size();
        }
        return 0;
    }

    private void onSelected(VariableViewHolder holder) {
        if (expand != holder.getAdapterPosition()) {
            notifyItemChanged(holder.getAdapterPosition());
            notifyItemChanged(expand);
            expand = holder.getAdapterPosition();
        } else {
            notifyItemChanged(expand);
            expand = -1;
        }
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(PARAM_EXPANDED, expand);
    }
}
