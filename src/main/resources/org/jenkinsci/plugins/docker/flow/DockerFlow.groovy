package org.jenkinsci.plugins.docker.flow

import hudson.FilePath

/**
 * Created by vfarcic on 08/03/16.
 */
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
        def flow = new Flow(this)
        flow.composePath = args.containsKey("composePath") ? args["composePath"] : "docker-compose.yml"
        flow.blueGreen = args.containsKey("blueGreen") ? args["blueGreen"] : true
        flow.target = args.containsKey("target") ? args["target"] : "" // Fail if empty
        flow.sideTargets = args.containsKey("sideTargets") ? args["sideTargets"] : []
        flow.pullTarget = args.containsKey("pullTarget") ? args["pullTarget"] : true
        flow.pullSideTargets = args.containsKey("pullSideTargets") ? args["pullSideTargets"] : false
        flow.project = args.containsKey("project") ? args["project"] : "" // Fail if empty
        flow.scAddress = args.containsKey("scAddress") ? args["scAddress"] : "" // Fail if empty
        flow.scale = args.containsKey("scale") ? args["scale"] : "1"
        flow.serviceName = flow.project + "-" + flow.target
        flow.currentColor = Consul.getCurrentColor(flow.scAddress, flow.serviceName)
        flow.nextColor = Consul.getNextColor(flow.currentColor)
        flow.currentTarget = Consul.getCurrentTarget(flow.target, flow.blueGreen, flow.currentColor)
        flow.nextTarget = Consul.getNextTarget(flow.target, flow.blueGreen, flow.nextColor)
        node {
            def dir = script.pwd()
            DockerCompose.createFlowFile(dir, flow.composePath, flow.blueGreen, flow.target, flow.nextTarget)
        }
        return flow
    }

    public static class Flow implements Serializable {

        private final DockerFlow dockerFlow
        public String composePath, target, project, scAddress, scale, serviceName
        public boolean blueGreen, pullTarget, pullSideTargets
        public String[] sideTargets
        private String currentColor = "green", nextColor = "blue"
        private String currentTarget, nextTarget
        public String dir

        private Flow(DockerFlow dockerFlow) {
            this.dockerFlow = dockerFlow
        }

        public void deploy() {
            dockerFlow.node {
                def command = DockerCompose.getPullTargetsCommand(
                        project,
                        nextTarget,
                        sideTargets,
                        pullTarget,
                        pullSideTargets
                )
                dockerFlow.script.sh ">> Deploying..."
                dockerFlow.script.sh "${command}"
            }
            Consul.putColor(scAddress, serviceName, nextColor)
        }

    }

//    deployed := false
//    for _, step := range opts.Flow {
//        switch strings.ToLower(step) {
//            case "scale":
//            if !deployed {
//                log.Println("Scaling...")
//                if err := dc.CreateFlowFile(opts.ComposePath, dockerComposeFlowPath, opts.Target, opts.CurrentColor, opts.BlueGreen); err != nil {
//                    log.Fatal(err)
//                }
//                if err := flow.Scale(opts, opts.ServiceDiscovery, dc, opts.CurrentTarget); err != nil {
//                    log.Fatal(err)
//                }
//            }
//            case "stop-old":
//            // TODO: Move to flow
//            if opts.BlueGreen {
//                target := opts.CurrentTarget
//                color := opts.CurrentColor
//                if !deployed {
//                    target = opts.NextTarget
//                    color = opts.NextColor
//                }
//                if err := dc.CreateFlowFile(opts.ComposePath, dockerComposeFlowPath, opts.Target, color, opts.BlueGreen); err != nil {
//                    log.Fatal(err)
//                }
//                if err := dc.StopTargets(opts.Host, opts.Project, []string{target}); err != nil {
//                    log.Fatal(err)
//                }
//            }
//            // TODO: End Move to flow
//        }
//    }
//
//    if err := dc.RemoveFlow(); err != nil {
//        log.Fatal(err)
//    }
}