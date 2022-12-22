# Gitlab initial password：docker exec -it gitlab grep 'Password:' /etc/gitlab/initial_root_password

# Pull image
resource "docker_image" "gitlab" {
  # (String) The name of the Docker image, including any tags or SHA256 repo digests.
  name = "gitlab/gitlab-ce:15.6.2-ce.0"
  # (Boolean) If true, then the Docker image won't be deleted on destroy operation.
  # If this is false, it will delete the image from the docker local storage on destroy operation.
  keep_locally = true
}

locals {
  container_name        = "gitlab"
  container_image       = docker_image.gitlab.name
  container_memory      = 12288
  container_memory_swap = 15360
  container_network     = data.terraform_remote_state.network.outputs.network[0]["name"]
  container_ip          = "172.18.0.3"
  container_ports = [
    {
      internal = 80
      external = 50080
    },
    {
      internal = 443
      external = 50443
    },
    {
      internal = 22
      external = 50022
    }
  ]
  container_volumes = [
    {
      host_path      = "/etc/localtime"
      container_path = "/etc/localtime"
    },
    {
      host_path      = "/data/gitlab/config"
      container_path = "/etc/gitlab"
    },
    {
      host_path      = "/data/gitlab/logs"
      container_path = "/var/log/gitlab"
    },
    {
      host_path      = "/data/gitlab/data"
      container_path = "/var/opt/gitlab"
    }
  ]
}

# Start a container
resource "docker_container" "gitlab" {
  name            = local.container_name
  image           = local.container_image
  memory          = local.container_memory
  memory_swap     = local.container_memory_swap
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