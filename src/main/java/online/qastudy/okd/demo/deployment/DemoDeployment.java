package online.qastudy.okd.demo.deployment;

import io.fabric8.openshift.api.model.RouteIngressConditionBuilder;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import lombok.extern.slf4j.Slf4j;
import online.qastudy.okd.demo.utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DemoDeployment {
    private static final String MASTER_URL = "https://127.0.0.1:8443";
    private static final String USERNAME = "developer";
    private static final String PASSWORD = "developer";
    private static final String POD_DEMO = "pod-demo";
    private static final int PORT = 8081;
    private static final String CLUSTER_IP = "172.30.152.124";
    private static final String DOCKER_IMAGE = "yemax/pod-demo:1";

    private DefaultKubernetesClient kubernetesClient;
    private Config config;
    private OpenShiftClient ocClient;
    private ProjectRequest request;


    public DemoDeployment(String namespace) {
        this.config = new ConfigBuilder()
                .withMasterUrl(MASTER_URL)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withNamespace(namespace)
                .withTrustCerts(true)
                .build();
        this.kubernetesClient = new DefaultKubernetesClient(config);
    }

    //    oc login -u developer -p developer
    public DemoDeployment login() {
        ocClient = kubernetesClient.adapt(OpenShiftClient.class);
        return this;
    }

    public DemoDeployment createNewProject(String projectName, String displayName, String description) {
        request = ocClient.projectrequests().createNew()
                .withNewMetadata()
                .withName(projectName)
                .endMetadata()
                .withDescription(description)
                .withDisplayName(displayName)
                .done();

        log.info("Project with name: {} ,was created.", request.getMetadata().getName());
        return this;
    }

    // oc new-app yemax/pod-demo:1 --name=pod-demo
    public DemoDeployment deployPod() {

        log.info("Namespace is: {}", ocClient.getNamespace());

        ocClient.deploymentConfigs().inNamespace(ocClient.getNamespace()).createOrReplaceWithNew()
                .withNewMetadata()
                .withLabels((Map<String, String>) new HashMap<>().put("app", "pod-name"))
                .withName(POD_DEMO)
                .withNamespace(ocClient.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withRevisionHistoryLimit(10)
                .withSelector(getPodSelectorsAsMap())
                .addNewTrigger()
                .withType("ConfigChange")
                .endTrigger()
                .addToSelector("app", POD_DEMO)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(getPodSelectorsAsMap())
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(POD_DEMO)
                .withImage(DOCKER_IMAGE)
                .addNewPort()
                .withContainerPort(8081)
                .withProtocol("TCP")
                .endPort()
                .endContainer()
                .withDnsPolicy("ClusterFirst")
                .withRestartPolicy("Always")
                .endSpec()
                .endTemplate()
                .addNewTrigger()
                .withType("ConfigChange")
                .endTrigger()
                .endSpec()
                .withNewStatus()
                .withAvailableReplicas(1)
                .withConditions(
                        new DeploymentConditionBuilder().withStatus("True").withType("Admitted").build(),
                        new DeploymentConditionBuilder().withStatus("True").withType("Progressing").build()
                )
                .endStatus()
                .done();
        return this;
    }

    private Map<String, String> getPodSelectorsAsMap() {
        Map<String, String> podSelectors = new HashMap<>();
        podSelectors.put("app", POD_DEMO);
        podSelectors.put("deploymentconfig", POD_DEMO);
        return podSelectors;
    }

    // oc expose svc/pod-demo-oc
    public DemoDeployment deployService() {
        Map<String, String> serviceSelector = getPodSelectorsAsMap();

        ocClient.services().createOrReplaceWithNew()
                .withNewMetadata()
                .withLabels((Map<String, String>) new HashMap<>().put("app", "pod-name"))
                .withName(POD_DEMO)
                .withNamespace(ocClient.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withClusterIP(CLUSTER_IP)
                .withPorts(new ServicePortBuilder()
                        .withName(PORT + "-tcp")
                        .withPort(PORT)
                        .withProtocol("TCP")
                        .withNewTargetPort()
                        .withIntVal(PORT)
                        .endTargetPort().build())
                .withSelector(serviceSelector)
                .withType("ClusterIP").endSpec().done();
        return this;
    }

    public DemoDeployment createRout() {
        ocClient.routes().createOrReplaceWithNew()
                .withNewMetadata()
                .withLabels((Map<String, String>) new HashMap<>().put("app", POD_DEMO))
                .withName(POD_DEMO)
                .withNamespace(ocClient.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withHost(Util.APP_URL)
                .withNewPort()
                .withNewTargetPort()
                .withIntVal(8081)
                .endTargetPort()
                .endPort()
                .withNewTo()
                .withKind("Service")
                .withName(POD_DEMO)
                .withWeight(100)
                .endTo()
                .withWildcardPolicy("None")
                .endSpec()
                .withNewStatus()
                .addNewIngress()
                .withConditions(new RouteIngressConditionBuilder().withStatus("True").withType("Admitted").build())
                .withHost(Util.APP_URL)
                .withRouterName("router")
                .withWildcardPolicy("None")
                .endIngress()
                .endStatus()
                .done();
        return this;
    }

    public String getApplicationURL() {
        String applicationUrl = Util.HTTP + ocClient.routes().list().getItems().stream()
                .filter(route -> route.getMetadata().getName().toLowerCase().equals(POD_DEMO))
                .collect(Collectors.toList()).stream().findFirst().get().getSpec().getHost();

        log.info("Application url: {}", applicationUrl);

        return applicationUrl;
    }

    public void close() {
        log.info("Close connection.");
        ocClient.close();
    }
}
