package org.jenkinsci.plugins.docker.flow;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Consul implements Serializable {

    protected boolean test(String address) {
        try {
            sendGetRequest(address + "/v1/status/leader");
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    protected String getCurrentColor(String address, String serviceName) {
        try {
            return sendGetRequest(getUrl(address, serviceName, "color"));
        } catch(Exception e) { }
        return "green";
    }

    protected String getNextColor(String currentColor) {
        return ("green".equals(currentColor)) ? "blue" : "green";
    }

    protected String getCurrentTarget(String target, boolean blueGreen, String currentColor) {
        if (blueGreen) {
            return target + "-" + currentColor;
        } else {
            return target;
        }
    }

    protected String getNextTarget(String target, boolean blueGreen, String nextColor) {
        if (blueGreen) {
            return target + "-" + nextColor;
        } else {
            return target;
        }
    }

    protected void putColor(String address, String serviceName, String value) throws IOException {
        putValue(address, serviceName, "color", value);
    }

    protected void putScale(String address, String serviceName, String value) throws IOException {
        putValue(address, serviceName, "scale", value);
    }

    protected int getScaleCalc(String address, String serviceName, String scale) {
        int s = 1, inc = 0;
        String resp = "";
        try {
            resp = sendGetRequest(getUrl(address, serviceName, "scale"));
        } catch(Exception e) { }
        if (resp.length() > 0) {
            s = Integer.parseInt(resp);
        }
        if (scale.length() > 0) {
            if (scale.startsWith("+") || scale.startsWith("-")) {
                inc = Integer.parseInt(scale);
            } else {
                s = Integer.parseInt(scale);
            }
        }
        int total = s + inc;
        if (total <= 0) {
            return 1;
        }
        return total;
    }

    // Util

    private String getUrl(String address, String serviceName, String key) {
        return address + "/v1/kv/docker-flow/" + serviceName + "/" + key + "?raw";
    }

    protected String sendGetRequest(String address) throws IOException {
        URL url = new URL(address);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String response = "";
        String line;
        while (null != (line = reader.readLine())) {
            response += line;
        }
        return response;
    }

    protected void putValue(String address, String serviceName, String key, String value) throws IOException {
        URL url = new URL(address + "/v1/kv/docker-flow/" + serviceName + "/" + key);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        OutputStream out = conn.getOutputStream();
        out.write(value.getBytes());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        readStream(reader);
        out.flush();
        out.close();
    }

    private String readStream(BufferedReader reader) throws IOException {
        String text = "";
        String line;
        while (null != (line = reader.readLine())) {
            text += line;
        }
        return text;
    }

}
