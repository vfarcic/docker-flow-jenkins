package org.jenkinsci.plugins.dockerflow;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.WithoutJenkins;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ConsulTest {

    private Consul consul;
    private final String ADDRESS = "http://my-consul.io/";
    private final String CURRENT_COLOR = "orange";
    private final String SERVICE_NAME = "myService";
    private final int SCALE = 23;
    private final int SCALE_FROM_CONSUL = 123;
    private final String URL_SCALE = ADDRESS + "/v1/kv/docker-flow/" + SERVICE_NAME + "/scale";

    @Before
    public void before() throws IOException {
        consul = spy(new Consul());
        doReturn("OK").when(consul).sendGetRequest(ADDRESS + "/v1/status/leader");
        doReturn(Integer.toString(SCALE_FROM_CONSUL)).when(consul).sendGetRequest(URL_SCALE + "?raw");
        doReturn(CURRENT_COLOR).when(consul).sendGetRequest(ADDRESS + "/v1/kv/docker-flow/" + SERVICE_NAME + "/color?raw");
        doNothing().when(consul).putValue(anyString(), anyString(), anyString(), anyString());
    }

    // test

    @Test @WithoutJenkins
    public void test_ReturnsTrue() throws IOException {
        boolean actual = consul.test(ADDRESS);

        assertTrue(actual);
    }

    @Test @WithoutJenkins
    public void test_ReturnsFalse_WhenException() throws IOException {
        doThrow(Exception.class).when(consul).sendGetRequest(anyString());

        boolean actual = consul.test(ADDRESS);

        assertFalse(actual);
    }

    // getCurrentColor

    @Test @WithoutJenkins
    public void getCurrentColor_ReturnsValueFromConsul() {
        String actual = consul.getCurrentColor(ADDRESS, SERVICE_NAME);

        assertEquals(CURRENT_COLOR, actual);
    }

    @Test @WithoutJenkins
    public void getCurrentColor_ReturnsGreen_WhenException() throws IOException {
        doThrow(Exception.class).when(consul).sendGetRequest(anyString());

        String actual = consul.getCurrentColor(ADDRESS, SERVICE_NAME);

        assertEquals("green", actual);
    }

    // getNextColor

    @Test @WithoutJenkins
    public void getNextColor_ReturnsBlue_WhenCurrentIsGreen() {
        String actual = consul.getNextColor("green");

        assertEquals("blue", actual);
    }

    @Test @WithoutJenkins
    public void getNextColor_ReturnsGreen_WhenCurrentIsNotGreen() {
        String actual = consul.getNextColor("orange");

        assertEquals("green", actual);
    }

    // getNextTarget

    @Test @WithoutJenkins
    public void getNextTarget_ReturnsTarget() {
        String expected = "myTarget";

        String actual = consul.getNextTarget(expected, false, "pink");

        assertEquals(expected, actual);
    }

    @Test @WithoutJenkins
    public void getNextTarget_ReturnsTargetWithColor_WhenBlueGreen() {
        String target = "myTarget";
        String color = "black";
        String expected = target + "-" + color;

        String actual = consul.getNextTarget(target, true, color);

        assertEquals(expected, actual);
    }

    // putColor

    @Test @WithoutJenkins
    public void putColor_InvokesPutValue() throws IOException {
        String value = "123";

        consul.putColor(ADDRESS, SERVICE_NAME, value);

        verify(consul).putValue(ADDRESS, SERVICE_NAME, "color", value);
    }

    // putScale

    @Test @WithoutJenkins
    public void putScale_InvokesPutValue() throws IOException {
        String value = "123";

        consul.putScale(ADDRESS, SERVICE_NAME, value);

        verify(consul).putValue(ADDRESS, SERVICE_NAME, "scale", value);
    }

    // getScaleCalc

    @Test @WithoutJenkins
    public void getScaleCalc_ReturnsValueFromConsul() {
        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, "");

        assertEquals(SCALE_FROM_CONSUL, actual);
    }

    @Test @WithoutJenkins
    public void getScaleCalc_Returns1_WhenValueFromConsulIsEmpty() throws IOException {
        doReturn("").when(consul).sendGetRequest(URL_SCALE + "?raw");

        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, "");

        assertEquals(1, actual);
    }

    @Test @WithoutJenkins
    public void getScaleCalc_Returns1_WhenConsulThrowsException() throws IOException {
        doThrow(IOException.class).when(consul).sendGetRequest(URL_SCALE + "?raw");

        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, "");

        assertEquals(1, actual);
    }

    @Test @WithoutJenkins
    public void getScaleCalc_ReturnsScale_WhenArgumentIsNotEmpty() throws IOException {
        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, Integer.toString(SCALE));

        assertEquals(SCALE, actual);
    }

    @Test @WithoutJenkins
    public void getScaleCalc_ReturnsScaleIncreased_WhenArgumentStartsWithPlusSign() throws IOException {
        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, "+" + SCALE);

        assertEquals(SCALE_FROM_CONSUL + SCALE, actual);
    }

    @Test @WithoutJenkins
    public void getScaleCalc_ReturnsScaleDecreased_WhenArgumentStartsWithMinusSign() throws IOException {
        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, "-" + SCALE);

        assertEquals(SCALE_FROM_CONSUL - SCALE, actual);
    }

    @Test @WithoutJenkins
    public void getScaleCalc_Returns1_WhenCalculatedValueIsNegativeOr0() throws IOException {
        int actual = consul.getScaleCalc(ADDRESS, SERVICE_NAME, "-" + (SCALE_FROM_CONSUL * 2));

        assertEquals(1, actual);
    }


}
