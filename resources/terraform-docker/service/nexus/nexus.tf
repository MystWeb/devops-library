# First execution：mkdir -p /data/nexus/data && chmod 777 -R /data/nexus
# initial password：docker exec -it nexus tail /nexus-data/admin.password

# Pull image
resource "docker_image" "nexus" {
  # (String) The name of the Docker image, including any tags or SHA256 repo digests.
  name         = "sonatype/nexus3:3.50.0"
  # (Boolean) If true, then the Docker image won't be deleted on destroy operation.
  # If this is false, it will delete the image from the docker local storage on destroy operation.
  keep_locally = true
}

locals {
  container_name        = "nexus"
  container_image       = docker_image.nexus.name
  container_memory      = 12288
  container_memory_swap = 15360
  container_privileged  = true
  container_network     = data.terraform_remote_state.network.outputs.network[0]["name"]
  container_ip          = "172.18.0.4"
  container_ports       = [
    {
      internal = 8081
      external = 8081
    }
  ]
  container_volumes = [
    {
      host_path      = "/etc/localtime"
      container_path = "/etc/localtime"
    },
    {
      host_path      = "/data/nexus/data"
      container_path = "/nexus-data"
    }
  ]
}

# Start a container
resource "docker_container" "nexus" {
  name            = local.container_name
  image           = local.container_image
  memory          = local.container_memory
  memory_swap     = local.container_memory_swap
  privileged      = local.container_privileged
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