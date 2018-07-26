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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.Action;
import com.imaginfire.uconfig.model.SchemaViewModel;

class ActionAdapter extends BaseAdapter<ActionViewHolder> {
    private static final String TAG = "ActionAdaptor";
    private static final String PARAM_EXPANDED = "com.imaginfire.uconfig.editor.ActionAdapter.EXPANDED";

    @NonNull
    private final SchemaViewModel model;
    private final LifecycleOwner owner;
    private int expand = -1;

    ActionAdapter(@NonNull LifecycleOwner owner, @NonNull SchemaViewModel schema, Bundle savedInstanceState) {
        model = schema;
        this.owner = owner;
        model.getVariables().observe(owner, list -> this.notifyDataSetChanged());

        if (savedInstanceState != null) {
            expand = savedInstanceState.getInt(PARAM_EXPANDED, -1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (model.getActions().getValue() != null) {
            return model.getActions().getValue().get(position).getParameterSpecification().size();
        }
        return 0;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.action_view, parent, false);
        ViewGroup param_parent = v.findViewById(R.id.parameter_pane);
        for (int i = 0; i < viewType; i++) {
            LayoutInflater.from(param_parent.getContext())
                    .inflate(R.layout.parameter_view, param_parent, true);
        }
        ActionViewHolder holder = new ActionViewHolder(owner, v);
        v.setOnClickListener( view -> {
            if (parent instanceof RecyclerView) {
                if (model.getActions().getValue() != null) {
                    onSelected(holder);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        if (model.getActions().getValue() != null) {
            Action a = model.getActions().getValue().get(position);
            a.getBusy().observe(holder, holder::setLoading);
            holder.setActionName(a.toString());
            holder.setExpanded(expand == position);
            holder.setParameters(a.getParameterSpecification());
            holder.setOnClickListener(v -> {
                if (!a.invoke(holder.getParameters(), () -> {
                    //ok
                    notifyItemChanged(expand);
                    expand = -1;
                    synchronized (this) {
                        if (recyclerView != null) {
                            Snackbar.make(recyclerView, R.string.snack_invoke_ok, Snackbar.LENGTH_LONG).show();
                        }
                    }
                }, () -> {
                    // fail
                    synchronized (this) {
                        if (recyclerView != null) {
                            Snackbar.make(holder.itemView, R.string.snack_invoke_fail, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                })) {
                    Snackbar.make(holder.itemView, R.string.snack_invoke_invalid, Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (model.getActions().getValue() != null) {
            return model.getActions().getValue().size();
        }
        return 0;
    }

    private void onSelected(ActionViewHolder holder) {
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
