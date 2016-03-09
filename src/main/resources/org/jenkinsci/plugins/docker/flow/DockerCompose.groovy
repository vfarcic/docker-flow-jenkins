package org.jenkinsci.plugins.docker.flow

/**
 * Created by vfarcic on 08/03/16.
 */
class DockerCompose implements Serializable {

    protected final static FLOW_PATH = "docker-flow.yml"

    // TODO: Write user-friendly error messages
    protected static void createFlowFile(String workspace, String composePath, boolean blueGreen, String target, String nextTarget) {
        def composeContent = new File(workspace + "/" + composePath).text
        if (blueGreen) {
            composeContent = composeContent.replace(target + ":", nextTarget + ":")
        }
        new File(workspace + "/" + FLOW_PATH).write(composeContent)
    }

    protected static String getPullTargetsCommand(
            String project,
            String nextTarget,
            String[] sideTargets,
            boolean pullTarget,
            boolean pullSideTargets
    ) {
        def targets = ""
        if (pullTarget) {
            targets += "${nextTarget} "
        }
        if (pullSideTargets) {
            targets += sideTargets.join(" ")
        }

        return getCommand(project, "pull " + targets)
    }

    private static String getCommand(String project, String args) {
        def argsString = "docker-compose -f ${FLOW_PATH}"
        if (project.length() > 0) {
            argsString += " -p ${project}"
        }
        return argsString + " " + args
    }

}