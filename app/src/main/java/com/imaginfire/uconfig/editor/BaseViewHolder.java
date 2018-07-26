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

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

abstract class BaseViewHolder extends RecyclerView.ViewHolder implements LifecycleOwner {
    private LifecycleRegistry lifecycle;
    private final LifecycleOwner owner;
    private final LifecycleObserver observer = new GenericLifecycleObserver() {
        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (lifecycle != null) {
                lifecycle.handleLifecycleEvent(event);
            }
        }
    };

    BaseViewHolder(LifecycleOwner owner, View itemView) {
        super(itemView);
        this.owner = owner;
        newlifecycle();
    }

    private void newlifecycle() {
        lifecycle = new LifecycleRegistry(this);
        owner.getLifecycle().addObserver(observer);
    }

    // to be called when viewholder is removed from the recycler to
    // detach all livedata observers
    void resetObservers() {
        owner.getLifecycle().removeObserver(observer);
        // LiveData observers are detached on an ON_DESTROY event
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        newlifecycle();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycle;
    }
}
