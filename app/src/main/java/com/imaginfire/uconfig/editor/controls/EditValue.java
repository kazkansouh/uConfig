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
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;

import com.imaginfire.uconfig.model.Value;

public class EditValue extends AppCompatEditText {
    private static final String TAG = "EditValue";

    private Value.Type type = Value.Type.String;

    public EditValue(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        try {
            return Value.parse(type, getText().toString());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to coerce " + getText() + " into type: " + type.toString());
            return null;
        }
    }
}
