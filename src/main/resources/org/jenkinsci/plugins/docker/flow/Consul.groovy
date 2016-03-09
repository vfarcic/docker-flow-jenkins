package org.jenkinsci.plugins.docker.flow

/**
 * Created by vfarcic on 08/03/16.
 */
class Consul implements Serializable {

    protected static String getCurrentColor(String address, String serviceName) {
        if (!address.toLowerCase().startsWith("http")) {
            address = "http://" + address
        }
        try {
            return new URL("${address}/v1/kv/docker-flow/${serviceName}/color?raw").getText()
        } catch(e) {}
        return "green"
    }

    protected static String getNextColor(String currentColor) {
        return (currentColor == "green") ? "blue" : "green"
    }

    protected static String getCurrentTarget(String target, boolean blueGreen, String currentColor) {
        if (blueGreen) {
            return target + "-" + currentColor
        } else {
            return target
        }
    }

    protected static String getNextTarget(String target, boolean blueGreen, String nextColor) {
        if (blueGreen) {
            return target + "-" + nextColor
        } else {
            return target
        }
    }

    protected static void putColor(String address, String serviceName, String value) {
        putValue(address, serviceName, "color", value)
    }

    private static void putValue(String address, String serviceName, String key, String value) {
        def url = new URL("${address}/v1/kv/docker-flow/${serviceName}/${key}")
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection()
        httpCon.setDoOutput(true)
        httpCon.setRequestMethod("PUT")
        OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream())
        out.write(value)
        out.close()
        httpCon.getInputStream()
    }

}