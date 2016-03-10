package org.jenkinsci.plugins.docker.flow

class DockerFlow implements Serializable {

    private org.jenkinsci.plugins.workflow.cps.CpsScript script

    public DockerFlow(org.jenkinsci.plugins.workflow.cps.CpsScript script) {
        this.script = script
    }

    private <V> V node(Closure<V> body) {
        if (script.env.HOME != null) { // http://unix.stackexchange.com/a/123859/26736
            // Already inside a node block.
            body()
        } else {
            script.node {
                body()
            }
        }
    }

    public Flow init(Map args) {
        def consul = new Consul()
        def compose = new DockerCompose();
        def flow = new Flow(this, consul, compose)
        flow.composePath = args.containsKey("composePath") ? args["composePath"] : "docker-compose.yml"
        flow.blueGreen = args.containsKey("blueGreen") ? args["blueGreen"] : true
        flow.target = args.containsKey("target") ? args["target"] : ""
        flow.sideTargets = args.containsKey("sideTargets") ? args["sideTargets"] : []
        flow.pullTarget = args.containsKey("pullTarget") ? args["pullTarget"] : true
        flow.pullSideTargets = args.containsKey("pullSideTargets") ? args["pullSideTargets"] : false
        flow.project = args.containsKey("project") ? args["project"] : ""
        flow.scAddress = args.containsKey("scAddress") ? args["scAddress"] : ""
        flow.scale = args.containsKey("scale") ? args["scale"] : "1"
        flow.serviceName = flow.project + "-" + flow.target
        flow.currentColor = consul.getCurrentColor(flow.scAddress, flow.serviceName)
        flow.nextColor = consul.getNextColor(flow.currentColor)
        flow.currentTarget = consul.getCurrentTarget(flow.target, flow.blueGreen, flow.currentColor)
        flow.nextTarget = consul.getNextTarget(flow.target, flow.blueGreen, flow.nextColor)
        if (flow.target.length() == 0 || flow.project.length() == 0 || flow.scAddress.length() == 0) {
            script.error "Following dockerFlow.init arguments are mandatory: target, project, and scAddress."
        }
        if (!consul.test(flow.scAddress)) {
            script.error "Could not connect to Consul on ${flow.scAddress}."
        }
        return flow
    }

    public static class Flow implements Serializable {

        private final DockerFlow dockerFlow
        private final Consul consul
        private final DockerCompose compose
        private String currentColor = "green", nextColor = "blue"
        private String currentTarget, nextTarget
        private boolean deployed

        public String composePath, target, project, scAddress, scale, serviceName
        public boolean blueGreen, pullTarget, pullSideTargets
        public String[] sideTargets

        private Flow(DockerFlow dockerFlow, Consul consul, DockerCompose compose) {
            this.dockerFlow = dockerFlow
            this.consul = consul
            this.compose = compose
        }

        public void deploy() {
            dockerFlow.node {
                String dir = dockerFlow.script.pwd()
                compose.createFlowFile(dir, composePath, blueGreen, target, nextTarget)
                dockerFlow.script.sh ">> Deploying..."
                dockerFlow.script.sh compose.getPullCommand(
                        project,
                        nextTarget,
                        sideTargets,
                        pullTarget,
                        pullSideTargets
                )
                dockerFlow.script.sh compose.getUpCommand(
                        project,
                        sideTargets
                )
                if (blueGreen) {
                    dockerFlow.script.sh compose.getRmCommand(
                            project,
                            nextTarget
                    )
                }
                def scaleCalc = consul.getScaleCalc(scAddress, serviceName, scale)
                dockerFlow.script.sh compose.getScaleCommand(
                        project,
                        nextTarget,
                        scaleCalc
                )
                consul.putColor(scAddress, serviceName, nextColor)
                consul.putScale(scAddress, serviceName, scaleCalc.toString())
                deployed = true
            }
        }

        public void scale() {
            dockerFlow.node {
                dockerFlow.script.sh ">> Scaling..."
                String dir = dockerFlow.script.pwd()
                compose.createFlowFile(dir, composePath, blueGreen, target, currentTarget)
                def scaleCalc = consul.getScaleCalc(scAddress, serviceName, scale)
                dockerFlow.script.sh compose.getScaleCommand(
                        project,
                        currentTarget,
                        scaleCalc
                )
                consul.putScale(scAddress, serviceName, scaleCalc.toString())
            }
        }

        public void stopOld() {
            dockerFlow.node {
                if (blueGreen) {
                    dockerFlow.script.sh ">> Stopping old..."
                    String dir = dockerFlow.script.pwd()
                    def stopTarget = (deployed) ? currentTarget : nextTarget
                    compose.createFlowFile(dir, composePath, blueGreen, target, stopTarget)
                    dockerFlow.script.sh compose.getStopCommand(
                            project,
                            stopTarget
                    )
                }
            }
        }

    }

}