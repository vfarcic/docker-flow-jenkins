package org.jenkinsci.plugins.dockerflow;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DockerComposeTest {

    private DockerCompose compose;
    private final String COMPOSE_CONTENT =
            "version: '2'\n" +
            "\n" +
            "services:\n" +
            "  app:\n" +
            "    image: vfarcic/books-ms\n" +
            "    ports:\n" +
            "      - 8080\n" +
            "    environment:\n" +
            "      - SERVICE_NAME=books-ms\n" +
            "      - DB_HOST=books-ms-db";
    private final String WORKSPACE = "path/to/jenkins/workspace";
    private final String COMPOSE_PATH = "docker-compose.yml";
    private final String TARGET = "app";
    private final String NEXT_TARGET = "myNextTarget";
    private final String SIDE_TARGETS_STRING = "sideTarget1 sideTarget2";
    private final String PROJECT = "myProject";
    private final String ARGS = "-a b -long not_much";
    private final int SCALE = 123;

    @Before
    public void before() throws IOException {
        compose = spy(new DockerCompose());
        doReturn(COMPOSE_CONTENT).when(compose).readFile(WORKSPACE + "/" + COMPOSE_PATH);
        doNothing().when(compose).writeFile(anyString(), anyString());
    }

    // createFlowFile

    @Test @WithoutJenkins
    public void createFlowFile_WritesComposeToFlow() throws IOException {
        compose.createFlowFile(WORKSPACE, COMPOSE_PATH, false, TARGET, NEXT_TARGET);

        verify(compose).writeFile(WORKSPACE + "/" + compose.FLOW_PATH, COMPOSE_CONTENT);
    }

    @Test @WithoutJenkins
    public void createFlowFile_ReplacesTargetWithNextTarget_WhenBlueGreen() throws IOException {
        String expected = COMPOSE_CONTENT.replace(TARGET + ":", NEXT_TARGET + ":");

        compose.createFlowFile(WORKSPACE, COMPOSE_PATH, true, TARGET, NEXT_TARGET);

        verify(compose).writeFile(WORKSPACE + "/" + compose.FLOW_PATH, expected);
    }

    // getCommand

    @Test @WithoutJenkins
    public void getCommand_StartsWithDockerComposeWithFlowPath() {
        String expected = "docker-compose -f " + compose.FLOW_PATH;

        String actual = compose.getCommand(PROJECT, "");

        assertTrue(actual.startsWith(expected));
    }

    @Test @WithoutJenkins
    public void getCommand_IncludesProject() {
        String actual = compose.getCommand(PROJECT, "");

        assertTrue(actual.contains(" -p " + PROJECT));
    }

    @Test @WithoutJenkins
    public void getCommand_DoesNotIncludeProject_WhenEmpty() {
        String actual = compose.getCommand("", "");

        assertFalse(actual.contains(" -p "));
    }

    @Test @WithoutJenkins
    public void getCommand_EndsWithArguments() {
        String actual = compose.getCommand(PROJECT, ARGS);

        assertTrue(actual.endsWith(ARGS));
    }

    // getPullCommand

    @Test @WithoutJenkins
    public void getPullCommand_ReturnsNextTarget_WhenPullTarget() {
        String expected = compose.getCommand(PROJECT, "pull " + NEXT_TARGET);

        String actual = compose.getPullCommand(PROJECT, NEXT_TARGET, SIDE_TARGETS_STRING.split(" "), true, false);

        assertEquals(expected, actual);
    }

    @Test @WithoutJenkins
    public void getPullCommand_ReturnsSideTargets_WhenPullSideTargets() {
        String expected = compose.getCommand(PROJECT, "pull " + SIDE_TARGETS_STRING);

        String actual = compose.getPullCommand(PROJECT, NEXT_TARGET, SIDE_TARGETS_STRING.split(" "), false, true);

        assertEquals(expected, actual);
    }

    @Test @WithoutJenkins
    public void getPullCommand_ReturnsAllTargets_WhenAllPulls() {
        String expected = compose.getCommand(PROJECT, "pull " + NEXT_TARGET + " " + SIDE_TARGETS_STRING);

        String actual = compose.getPullCommand(PROJECT, NEXT_TARGET, SIDE_TARGETS_STRING.split(" "), true, true);

        assertEquals(expected, actual);
    }

    // getUpCommand

    @Test @WithoutJenkins
    public void getUpCommand_ReturnsUpWithAllTargets() {
        String targets = NEXT_TARGET + " " + SIDE_TARGETS_STRING;
        String expected = compose.getCommand(PROJECT, "up -d " + targets);

        String actual = compose.getUpCommand(PROJECT, targets.split(" "));

        assertEquals(expected, actual);
    }

    // getRmCommand

    @Test @WithoutJenkins
    public void getRmCommand_ReturnsRmWithAllTargets() {
        String targets = NEXT_TARGET + " " + SIDE_TARGETS_STRING;
        String expected = compose.getCommand(PROJECT, "rm -f " + targets);

        String actual = compose.getRmCommand(PROJECT, targets.split(" "));

        assertEquals(expected, actual);
    }

    // getScaleCommand

    @Test @WithoutJenkins
    public void getScaleCommand_ReturnsScale() {
        String expected = compose.getCommand(PROJECT, "scale " + NEXT_TARGET + "=" + SCALE);

        String actual = compose.getScaleCommand(PROJECT, NEXT_TARGET, SCALE);

        assertEquals(expected, actual);
    }

    // getRmCommand

    @Test @WithoutJenkins
    public void getStopCommand_ReturnsStop() {
        String expected = compose.getCommand(PROJECT, "stop " + NEXT_TARGET);

        String actual = compose.getStopCommand(PROJECT, NEXT_TARGET);

        assertEquals(expected, actual);
    }

}
