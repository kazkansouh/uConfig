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

package com.imaginfire.uconfig.model;

import android.support.annotation.NonNull;

public class Value {
    public enum Type {
        Byte , Int, String
    }

    private final Integer ui8;
    private final Integer i;
    private final String s;

    public final Type type;

    Value(Integer ui8, Integer i, String s) {
        if (ui8 != null) {
            type = Type.Byte;
            this.ui8 = ui8;
            this.i = null;
            this.s = null;
            return;
        }
        if (i != null) {
            type = Type.Int;
            this.ui8 = null;
            this.i = i;
            this.s = null;
            return;
        }
        if (s != null) {
            type = Type.String;
            this.ui8 = null;
            this.i = null;
            this.s = s;
            return;
        }
        throw new RuntimeException("Uninitialised value");
    }

    public Integer getByteValue() {
        if (type == Type.Byte && ui8 != null) {
            return ui8;
        }
        if (type == Type.Int && i != null) {
            if (i > 0xFF) {
                return 0xFF;
            } else {
                return i;
            }
        }
        if (type == Type.String && s != null) {
            try {
                Integer i = Integer.decode(s);
                if (i < 0) {
                    i = 0;
                }
                if (i > 0xFF) {
                    i = 0xFF;
                }
                return i;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getIntegerValue() {
        if (type == Type.Byte && ui8 != null) {
            return ui8;
        }
        if (type == Type.Int && i != null) {
            return i;
        }
        if (type == Type.String && s != null) {
            try {
                return Integer.decode(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String getStringValue() {
        if (type == Type.Byte && ui8 != null) {
            return ui8.toString();
        }
        if (type == Type.Int && i != null) {
            return i.toString();
        }
        if (type == Type.String && s != null) {
            return s;
        }
        return null;
    }

    @Override
    public String toString() {
        return getStringValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Value) {
            Value v = (Value)obj;
            if (v.type == type) {
                switch (type) {
                    case Byte:
                        if (ui8 != null && v.ui8 != null) {
                            return ui8.equals(v.ui8);
                        }
                        break;
                    case Int:
                        if (i != null && v.i != null) {
                            return i.equals(v.i);
                        }
                        break;
                    case String:
                        if (s != null && v.s != null) {
                            return s.equals(v.s);
                        }
                        break;
                }
             }
        }
        return false;
    }

    public static Value parse(Type type, @NonNull String s) throws IllegalArgumentException {
        switch (type) {
            case Byte:
                int i = Integer.decode(s);
                if (i < 0 || i > 255) {
                    throw new NumberFormatException("Not a valid uint8 value: " + s);
                }
                return new Value(i, null, null);
            case Int:
                return new Value(null, Integer.decode(s), null);
            case String:
                if (s.equals("")) {
                    throw new IllegalArgumentException("Empty string is not valid.");
                }
                return new Value(null, null, s);
        }
        return null;
    }
}
