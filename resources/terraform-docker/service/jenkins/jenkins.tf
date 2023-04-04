# initial password：docker exec -it jenkins tail /var/jenkins_home/secrets/initialAdminPassword
# 中文插件：https://plugins.jenkins.io/locale
# RBAC权限管理插件：https://plugins.jenkins.io/role-strategy，配置：全局安全配置|授权策略|Role-Based Strategy
# Jenkins备份插件：https://plugins.jenkins.io/thinBackup/
# Jenkins UI插件：https://plugins.jenkins.io/blueocean/
# 通用WebHook触发器插件：https://plugins.jenkins.io/generic-webhook-trigger/
# Jenkins任务重建插件：https://plugins.jenkins.io/rebuild/
# Jenkins邮件插件：https://plugins.jenkins.io/email-ext/
# Pipeline工具箱：https://plugins.jenkins.io/pipeline-utility-steps/
# HttpRequest：https://plugins.jenkins.io/http_request

# Pull image
resource "docker_image" "jenkins" {
  # (String) The name of the Docker image, including any tags or SHA256 repo digests.
  name         = "jenkins/jenkins:2.387.1-lts-jdk11"
  # (Boolean) If true, then the Docker image won't be deleted on destroy operation.
  # If this is false, it will delete the image from the docker local storage on destroy operation.
  keep_locally = true
}

locals {
  container_name        = "jenkins"
  container_image       = docker_image.jenkins.name
  container_memory      = 8192
  container_memory_swap = 15360
  container_user        = "root"
  container_privileged  = true
  container_env         = [
    "JAVA_OPTS=-Dorg.apache.commons.jelly.tags.fmt.timeZone='Asia/Shanghai'",
    #    "JAVA_OPTS=-Duser.timezone='Asia/Shanghai'",
  ]
  container_network = data.terraform_remote_state.network.outputs.network[0]["name"]
  container_ip      = "172.18.0.3"
  container_ports   = [
    {
      internal = 8080
      external = 8080
    },
    {
      internal = 50000
      external = 50000
    }
  ]
  container_volumes = [
    {
      host_path      = "/etc/localtime"
      container_path = "/etc/localtime"
    },
    {
      host_path      = "/data/jenkins_home"
      container_path = "/var/jenkins_home"
    }
  ]
}

# Start a container
resource "docker_container" "jenkins" {
  name            = local.container_name
  image           = local.container_image
  memory          = local.container_memory
  memory_swap     = local.container_memory_swap
  user            = local.container_user
  privileged      = local.container_privileged
  env             = local.container_env
  max_retry_count = 3
  restart         = "on-failure"
  networks_advanced {
    name         = local.container_network
    ipv4_address = local.container_ip
  }

  dynamic "ports" {
    for_each = local.container_ports
    content {
      internal = ports.value.internal
      external = ports.value.external
      ip       = "0.0.0.0"
      protocol = "tcp"
    }
  }

  dynamic "volumes" {
    for_each = local.container_volumes
    content {
      host_path      = volumes.value.host_path
      container_path = volumes.value.container_path
    }
  }

}