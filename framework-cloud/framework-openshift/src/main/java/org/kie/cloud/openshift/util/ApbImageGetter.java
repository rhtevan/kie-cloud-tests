/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.cloud.openshift.util;

import org.kie.cloud.openshift.constants.OpenShiftConstants;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;

public class ApbImageGetter {

    private static OpenShiftClient openShiftClient;

    static {
        OpenShiftConfig openShiftConfig = new OpenShiftConfigBuilder()
                .withMasterUrl(OpenShiftConstants.getOpenShiftUrl())
                .withTrustCerts(true)
                .withDisableApiGroupCheck(true)
                .withUsername(OpenShiftConstants.getOpenShiftAdminUserName())
                .withPassword(OpenShiftConstants.getOpenShiftAdminPassword())
                .build();
        openShiftClient = new DefaultOpenShiftClient(openShiftConfig);
    }

    /**
     * OpenShift APB image stream name is set by property "apb.image.stream.name".
     * We can set docker image tag by the property "apb.image.docker.repo.tag". If
     * docker image tag is not set, then is used by default image with latest tag.
     * Returns docker image name of apb image in namespace "openshift".
     * 
     * @return Name of docker image in OpenShift registry.
     */
    public static String fromImageStream() {
        String dockerImage = openShiftClient.imageStreams()
                                .inNamespace("openshift")
                                .withName(OpenShiftConstants.getApbImageStreamName())
                                .get()
                                .getStatus()
                                .getDockerImageRepository();
        String tag = OpenShiftConstants.getApbImageDockerRepoTag();
        if (tag != null && !tag.isEmpty()) {
            return dockerImage + ":" + tag;
        } else {
            return dockerImage;
        }
    }
}
