package org.jenkinsci.plugins.docker.flow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DockerCompose implements Serializable {

    protected final String FLOW_PATH = "docker-flow.yml";

    protected void createFlowFile(
            String workspace,
            String composePath,
            boolean blueGreen,
            String target,
            String nextTarget
    ) throws IOException {
        String content = readFile(workspace + "/" + composePath);
        if (blueGreen) {
            content = content.replace(target + ":", nextTarget + ":");
        }
        writeFile(workspace + "/" + FLOW_PATH, content);
    }

    protected String getCommand(String project, String args) {
        String command = "docker-compose -f " + FLOW_PATH;
        if (project.length() > 0) {
            command += " -p " + project;
        }
        return (command + " " + args).trim();
    }

    protected String getPullCommand(
            String project,
            String nextTarget,
            String[] sideTargets,
            boolean pullTarget,
            boolean pullSideTargets
    ) {
        String targets = "";
        if (pullTarget) {
            targets += nextTarget + " ";
        }
        if (pullSideTargets) {
            for (String target : sideTargets) {
                targets += target + " ";
            }
        }

        return getCommand(project, "pull " + targets.trim());
    }

    protected String getUpCommand(String project, String[] targets) {
        String targetsString = "";
        for (String target : targets) {
            targetsString += target + " ";
        }
        return getCommand(project, "up -d " + targetsString.trim());
    }

    protected String getRmCommand(String project, String[] targets) {
        String targetsString = "";
        for (String target : targets) {
            targetsString += target + " ";
        }
        return getCommand(project, "rm -f " + targetsString);
    }

    protected String getScaleCommand(String project, String target, int scale) {
        return getCommand(project, "scale " + target + "=" + scale);
    }

    protected String getStopCommand(String project, String target) {
        return getCommand(project, "stop " + target);
    }

    // Util

    protected String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    protected void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(content);
        fileWriter.flush();
        fileWriter.close();
    }

}
