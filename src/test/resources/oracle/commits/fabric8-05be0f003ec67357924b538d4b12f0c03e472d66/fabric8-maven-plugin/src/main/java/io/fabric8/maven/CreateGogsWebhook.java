/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.repo.git.CreateWebhookDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.WebHookDTO;
import io.fabric8.repo.git.WebhookConfig;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 * Creates a web hook in a gogs repository
 */
@Mojo(name = "create-gogs-webhook", requiresProject = false)
public class CreateGogsWebhook extends AbstractNamespacedMojo {

    /**
     * The URL of the webhook to register
     */
    @Parameter(property = "webhookUrl", required = true)
    private String webhookUrl;

    /**
     * The gogs repo to add the webhook to
     */
    @Parameter(property = "repo", required = true)
    private String repo;

    /**
     * The user name to use in gogs
     */
    @Parameter(property = "gogsUsername", defaultValue = "${JENKINS_GOGS_USER}")
    private String gogsUsername;

    /**
     * The password to use in gogs
     */
    @Parameter(property = "gogsPassword", defaultValue = "${JENKINS_GOGS_PASSWORD}")
    private String gogsPassword;

    /**
     * The secret added to the webhook
     */
    @Parameter(property = "secret", defaultValue = "secret101")
    private String secret;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // lets add defaults if not env vars
        if (Strings.isNullOrBlank(gogsUsername)) {
            gogsUsername = "gogsadmin";
        }
        if (Strings.isNullOrBlank(gogsPassword)) {
            gogsPassword = "RedHat$1";
        }

        try {
            KubernetesClient kubernetes = getKubernetes();
            String namespace = kubernetes.getNamespace();
            Log log = getLog();
            String gogsAddress = kubernetes.getServiceURL(ServiceNames.GOGS, namespace, "http", true);
            log.info("Found gogs address: " + gogsAddress + " for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getAddress());
            if (Strings.isNullOrBlank(gogsAddress)) {
                throw new MojoExecutionException("No address for service " + ServiceNames.GOGS + " in namespace: "
                        + namespace  + " on Kubernetes address: " + kubernetes.getAddress());
            }
            log.info("Querying webhooks in gogs for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getAddress());

            GitRepoClient repoClient = new GitRepoClient(gogsAddress, gogsUsername, gogsPassword);
            List<WebHookDTO> webhooks = repoClient.getWebhooks(gogsUsername, repo);
            for (WebHookDTO webhook : webhooks) {
                String url = null;
                WebhookConfig config = webhook.getConfig();
                if (config != null) {
                    url = config.getUrl();
                    if (Objects.equal(webhookUrl, url)) {
                        log.info("Already has webhook for: " + url + " so not creating again");
                        return;
                    }
                    log.info("Ignoring webhook " + url + " from: " + toJson(config));
                }
            }
            CreateWebhookDTO createWebhook = new CreateWebhookDTO();
            createWebhook.setType("gogs");
            WebhookConfig config = createWebhook.getConfig();
            config.setUrl(webhookUrl);
            config.setSecret(secret);
            WebHookDTO webhook = repoClient.createWebhook(gogsUsername, repo, createWebhook);
            if (log.isDebugEnabled()) {
                log.debug("Got created web hook: " + toJson(webhook));
            }
            log.info("Created webhook for " + webhookUrl + " for namespace: " + namespace + " on gogs URL: " + gogsAddress);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }
}
