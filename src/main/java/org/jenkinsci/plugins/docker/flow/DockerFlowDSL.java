package org.jenkinsci.plugins.docker.flow;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

/**
 * Created by vfarcic on 08/03/16.
 */
@Extension
public class DockerFlowDSL extends GlobalVariable {

    @Override public String getName() {
        return "dockerFlow";
    }

    @Override public Object getValue(CpsScript script) throws Exception {
        Binding binding = script.getBinding();
        Object dockerFlow;
        if (binding.hasVariable(getName())) {
            dockerFlow = binding.getVariable(getName());
        } else {
            // Note that if this were a method rather than a constructor, we would need to mark it @NonCPS lest it throw CpsCallableInvocation.
            dockerFlow = script.getClass().getClassLoader()
                    .loadClass("org.jenkinsci.plugins.docker.flow.DockerFlow")
                    .getConstructor(CpsScript.class).newInstance(script);
            binding.setVariable(getName(), dockerFlow);
        }
        return dockerFlow;
    }

}
