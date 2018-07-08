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

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

class UDPReceiver extends Thread {
    private static final String TAG = "UDPReceiver";
    private DatagramSocket udp_socket = null;
    private OnEventListener listener = null;
    private boolean closed = false;

    void setEventListener(OnEventListener l) {
        listener = l;
    }

    @Override
    public void run() {
        try {
            // monitor udp port
            udp_socket = new DatagramSocket(8003);
            byte[] buf = new byte[256];
            Log.d(TAG, "starting udp server");
            while (!closed) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                udp_socket.receive(packet);
                Log.v(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
                JSONTokener json_parser = new JSONTokener(new String(packet.getData()));
                Object data = null;
                try {
                    data = json_parser.nextValue();
                } catch (JSONException e) {
                    Log.e(TAG, "malformed JSON received in udp packet", e);
                }

                if (data instanceof JSONObject) {
                    JSONObject payload = (JSONObject) data;
                    OnReceive(packet.getAddress(), payload);
                } else {
                    Log.e(TAG, "JSON object not received in udp packet");
                }
            }
        } catch (SocketException e) {
            OnError();
            Log.d(TAG, "error scanning network", e);
        } catch (IOException e) {
            Log.d(TAG, "stopping udp server", e);
        }
    }

    void shutdown() {
        closed = true;
        if (udp_socket != null) {
            udp_socket.close();
        }
    }

    private void OnReceive(InetAddress address, JSONObject payload) {
        if (listener != null) {
            listener.OnReceive(address, payload);
        }
    }

    private void OnError() {
        if (listener != null) {
            listener.OnError();
        }
    }

    interface OnEventListener {
        void OnReceive(InetAddress address, JSONObject payload);
        void OnError();
    }
}
