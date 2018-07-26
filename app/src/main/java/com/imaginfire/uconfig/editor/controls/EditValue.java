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

package com.imaginfire.uconfig.editor.controls;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;

import com.imaginfire.uconfig.R;
import com.imaginfire.uconfig.model.Value;

public class EditValue extends AppCompatEditText {
    private static final String TAG = "EditValue";

    private Value.Type type = Value.Type.String;
    private Value value = null;

    public EditValue(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Drawable ok = context.getResources().getDrawable(R.drawable.ic_check_circle_black_24dp, context.getTheme());
        final Drawable error = context.getResources().getDrawable(R.drawable.ic_warning_black_24dp, context.getTheme());

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    value = Value.parse(type, s.toString());
                    if (value != null) {
                        setCompoundDrawablesWithIntrinsicBounds(null, null, ok, null);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    setCompoundDrawablesWithIntrinsicBounds(null, null, error, null);
                }
                Log.w(TAG,"Unable to convert string into: " + type);
                setCompoundDrawablesWithIntrinsicBounds(null, null, error, null);
            }
        });
    }

    public void setType(Value.Type type) {
        this.type = type;
        switch (type) {
            case Byte:
            case Int:
                setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case String:
                setInputType(InputType.TYPE_CLASS_TEXT);
                break;
        }
    }

    public Value getValue() {
        return value;
    }

    public void setText(Value v) {
        setType(v.type);
        setText(v.getStringValue());
    }
}
