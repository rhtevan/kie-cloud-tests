/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.openshift.operator.scenario;

import java.io.IOException;
import java.util.List;

import cz.xtf.openshift.OpenShiftBinaryClient;
import cz.xtf.openshift.OpenShiftUtils;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import org.kie.cloud.api.scenario.DeploymentScenario;
import org.kie.cloud.openshift.constants.OpenShiftConstants;
import org.kie.cloud.openshift.operator.resources.OpenShiftResource;
import org.kie.cloud.openshift.resource.Project;
import org.kie.cloud.openshift.scenario.OpenShiftScenario;
import org.kie.cloud.openshift.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OpenShiftOperatorScenario<T extends DeploymentScenario<T>> extends OpenShiftScenario<T> {

    private static final Logger logger = LoggerFactory.getLogger(OpenShiftOperatorScenario.class);

    @Override
    protected void deployKieDeployments() {
        deployOperator();
        deployCustomResource();
    }

    private void deployOperator() {
        try {
            // Operations need to be done as an administrator
            OpenShiftBinaryClient.getInstance().login(OpenShiftConstants.getOpenShiftUrl(), OpenShiftConstants.getOpenShiftAdminUserName(), OpenShiftConstants.getOpenShiftAdminPassword(), null);

            createCustomResourceDefinitionsInOpenShift();
            createServiceAccountInProject(project);
            createRoleInProject(project);
            createRoleBindingsInProject(project);
            createOperatorInProject(project);
        } catch (IOException e) {
            throw new RuntimeException("Error while initializing Operator.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while initializing Operator.", e);
        }
    }

    private void createCustomResourceDefinitionsInOpenShift() {
        List<CustomResourceDefinition> customResourceDefinitions = OpenShiftUtils.admin().client().customResourceDefinitions().list().getItems();
        boolean operatorCrdExists = customResourceDefinitions.stream().anyMatch(i -> i.getMetadata().getName().equals("kieapps.app.kiegroup.org"));
        if(!operatorCrdExists) {
            logger.info("Creating custom resource definitions from " + OpenShiftResource.CRD.getResourceUrl().toString());
            OpenShiftBinaryClient instance = OpenShiftBinaryClient.getInstance();
            try (ProcessExecutor executor = new ProcessExecutor()) {
                executor.executeProcessCommand(instance.getOcBinaryPath().toString() + " --config=" + instance.getOcConfigPath().toString() + " create -n " + getNamespace() + " -f " + OpenShiftResource.CRD.getResourceUrl().toString());
            }
        }
    }

    private void createServiceAccountInProject(Project project) {
        logger.info("Creating service account in project '" + project.getName() + "' from " + OpenShiftResource.SERVICE_ACCOUNT.getResourceUrl().toString());
        OpenShiftBinaryClient.getInstance().project(project.getName());
        OpenShiftBinaryClient.getInstance().executeCommand("Service account creation failed.", "create", "-f", OpenShiftResource.SERVICE_ACCOUNT.getResourceUrl().toString());
    }

    private void createRoleInProject(Project project) {
        logger.info("Creating role in project '" + project.getName() + "' from " + OpenShiftResource.ROLE.getResourceUrl().toString());
        OpenShiftBinaryClient.getInstance().project(project.getName());
        OpenShiftBinaryClient.getInstance().executeCommand("Role creation failed.", "create", "-f", OpenShiftResource.ROLE.getResourceUrl().toString());
    }

    private void createRoleBindingsInProject(Project project) {
        logger.info("Creating role bindings in project '" + project.getName() + "' from " + OpenShiftResource.ROLE_BINDING.getResourceUrl().toString());
        OpenShiftBinaryClient.getInstance().project(project.getName());
        OpenShiftBinaryClient.getInstance().executeCommand("Role binding failed.", "create", "-f", OpenShiftResource.ROLE_BINDING.getResourceUrl().toString());
    }

    private void createOperatorInProject(Project project) {
        logger.info("Creating operator in project '" + project.getName() + "' from " + OpenShiftResource.OPERATOR.getResourceUrl().toString());
        OpenShiftBinaryClient.getInstance().project(project.getName());
        OpenShiftBinaryClient.getInstance().executeCommand("operator failed.", "create", "-f", OpenShiftResource.OPERATOR.getResourceUrl().toString());
        // wait until operator is ready
        project.getOpenShiftUtil().waiters().areExactlyNPodsRunning(1, "name", "kie-cloud-operator");
    }

    protected abstract void deployCustomResource();
}
