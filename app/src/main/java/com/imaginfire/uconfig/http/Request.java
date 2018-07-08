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

package com.imaginfire.uconfig.http;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

public class Request extends AsyncTask<RequestArguments, Void, JSONObject> {
    private final static String TAG = "Request";
    private OnDownloadCallback cb;

    private static final Object lock = new Object();

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
        cb.onDownload(jsonObject);
        cb = null;
    }

    @Override
    protected JSONObject doInBackground(RequestArguments... requestArguments) {
        RequestArguments a = requestArguments[0];
        cb = a.callback;
        try {
            StringBuilder sb_url = new StringBuilder();
            sb_url.append(a.url);
            if (a.params != null && !a.params.isEmpty()) {
                sb_url.append('?');
                for (Map.Entry<String, String> e : a.params.entrySet()) {
                    if (sb_url.charAt(sb_url.length()-1) != '?') {
                        sb_url.append('&');
                    }
                    sb_url.append(e.getKey());
                    sb_url.append('=');
                    sb_url.append(URLEncoder.encode(e.getValue(), "UTF-8"));
                }
            }
            Log.d(TAG,"Requesting: " + sb_url.toString());
            synchronized (lock) {
                URL url = new URL(sb_url.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                if (connection.getResponseCode() < 300) {
                    //Get Response
                    String response = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                            .lines().collect(Collectors.joining("\n"));
                    JSONTokener tok = new JSONTokener(response);
                    Object data = tok.nextValue();
                    if (data instanceof JSONObject) {
                        return (JSONObject) data;
                    } else {
                        Log.e(TAG, "Not a JSON object at root");
                    }
                } else {
                    Log.e(TAG, "Failed to get valid 200 response from server");
                }
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Schema url invalid", e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UTF is not supported.", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to communicate with server to read schema", e);
        } catch (JSONException e) {
            Log.e(TAG, "Could not parse returned data as json.", e);
        }
        return null;
    }
}
