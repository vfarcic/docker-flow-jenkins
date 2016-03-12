package org.jenkinsci.plugins.dockerflow;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

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
            dockerFlow = script.getClass()
                    .getClassLoader()
                    .loadClass("DockerFlow")
                    .getConstructor(CpsScript.class)
                    .newInstance(script);
            binding.setVariable(getName(), dockerFlow);
        }
        return dockerFlow;
    }

}
