package com.cookierdelivery.cdk.services;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;

public class MsProductsServiceStack extends Stack {

  private static final String SERVICE_ID = "ALB-MS-PRODUCTS";
  private static final String SERVICE_NAME = "ms-products";
  private static final int CPU_NUMBER = 512;
  private static final int INSTANCE_COUNT = 2;
  private static final int MEMORY_LIMIT = 1024;
  private static final int LISTENER_PORT = 8080;

  private static final int SCALE_MIN_INSTANCES = 2;
  private static final int SCALE_MAX_INSTANCES = 4;
  private static final int SCALE_TARGET_UTILIZATION_PERCENT = 50;
  private static final int SCALE_IN_COOLDOWN_PERIOD_SECONDS = 60;
  private static final int SCALE_OUT_COOLDOWN_PERIOD_SECONDS = 60;

  private static final String LOG_ID = "MsProducts";
  private static final String LOG_GROUP = "MsProducts";
  private static final String LOG_PREFIX = "MsProducts";

  private static final String JDBC_URL = "jdbc:postgresql://{0}:5432/ms_products";

  private static final String IMAGE_REPOSITORY = "luanelioliveira";
  private static final String IMAGE_NAME = "cookierdelivery-ms-products";
  private static final String IMAGE_TAG = "1.2.0";

  private static final String CONTAINER_NAME = "ms-products";
  private static final int CONTAINER_PORT = 8080;

  private static final String HEALTH_CHECK_PATH = "/management/health";
  private static final String HEALTH_CHECK_PORT = "8080";
  private static final String HEALTH_CHECK_HEALTHY_STATUS_CODE = "200";

  private static final String AUTOSCALING_ID = "MsProductsAutoScaling";

  public MsProductsServiceStack(final Construct scope, final String id, Cluster cluster) {
    this(scope, id, null, cluster);
  }

  public MsProductsServiceStack(
      final Construct scope, final String id, final StackProps props, Cluster cluster) {
    super(scope, id, props);

    ApplicationLoadBalancedFargateService applicationService = createApplicationService(cluster);
    configureHealthCheck(applicationService);
    configureScalable(applicationService);
  }

  private ApplicationLoadBalancedFargateService createApplicationService(Cluster cluster) {

    return ApplicationLoadBalancedFargateService.Builder.create(this, SERVICE_ID)
        .cluster(cluster)
        .serviceName(SERVICE_NAME)
        .desiredCount(INSTANCE_COUNT)
        .cpu(CPU_NUMBER)
        .memoryLimitMiB(MEMORY_LIMIT)
        .listenerPort(LISTENER_PORT)
        .taskImageOptions(taskImageOptions())
        .publicLoadBalancer(true)
        .build();
  }

  private AwsLogDriverProps logConfig() {

    LogGroup logGroup =
        LogGroup.Builder.create(this, LOG_ID)
            .logGroupName(LOG_GROUP)
            .removalPolicy(RemovalPolicy.DESTROY)
            .build();

    return AwsLogDriverProps.builder().logGroup(logGroup).streamPrefix(LOG_PREFIX).build();
  }

  private Map<String, String> createEnvironments() {
    Map<String, String> environments = new HashMap<>();

    String databaseUrl = MessageFormat.format(JDBC_URL, Fn.importValue("ms-products-db-url"));
    String databaseUsername = Fn.importValue("ms-products-db-username");
    String databasePassword = Fn.importValue("ms-products-db-password");

    environments.put("SPRING_DATASOURCE_URL", databaseUrl);
    environments.put("SPRING_DATASOURCE_USERNAME", databaseUsername);
    environments.put("SPRING_DATASOURCE_PASSWORD", databasePassword);

    return environments;
  }

  private ApplicationLoadBalancedTaskImageOptions taskImageOptions() {
    String image = MessageFormat.format("{0}/{1}:{2}", IMAGE_REPOSITORY, IMAGE_NAME, IMAGE_TAG);

    return ApplicationLoadBalancedTaskImageOptions.builder()
        .containerName(CONTAINER_NAME)
        .image(ContainerImage.fromRegistry(image))
        .containerPort(CONTAINER_PORT)
        .logDriver(LogDriver.awsLogs(logConfig()))
        .environment(createEnvironments())
        .build();
  }

  private void configureHealthCheck(ApplicationLoadBalancedFargateService loadBalancedService) {

    HealthCheck healthCheck =
        new HealthCheck.Builder()
            .path(HEALTH_CHECK_PATH)
            .port(HEALTH_CHECK_PORT)
            .healthyHttpCodes(HEALTH_CHECK_HEALTHY_STATUS_CODE)
            .build();

    loadBalancedService.getTargetGroup().configureHealthCheck(healthCheck);
  }

  private void configureScalable(ApplicationLoadBalancedFargateService loadBalancedService) {

    ScalableTaskCount scalableTaskCount =
        loadBalancedService
            .getService()
            .autoScaleTaskCount(
                EnableScalingProps.builder()
                    .minCapacity(SCALE_MIN_INSTANCES)
                    .maxCapacity(SCALE_MAX_INSTANCES)
                    .build());

    CpuUtilizationScalingProps cpuUtilizationScalingProps =
        CpuUtilizationScalingProps.builder()
            .targetUtilizationPercent(SCALE_TARGET_UTILIZATION_PERCENT)
            .scaleInCooldown(Duration.seconds(SCALE_IN_COOLDOWN_PERIOD_SECONDS))
            .scaleOutCooldown(Duration.seconds(SCALE_OUT_COOLDOWN_PERIOD_SECONDS))
            .build();

    scalableTaskCount.scaleOnCpuUtilization(AUTOSCALING_ID, cpuUtilizationScalingProps);
  }
}
