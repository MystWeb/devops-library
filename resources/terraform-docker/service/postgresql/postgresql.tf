### 创建一个sonar用户与数据库
# docker exec -it postgresql bash
# psql -U postgres -d postgres -h 127.0.0.1 -p 5432
# create user sonar with password 'sonar';
# create database sonar owner sonar;
# grant all privileges on database sonar to sonar;

# Pull image
resource "docker_image" "postgresql" {
  # (String) The name of the Docker image, including any tags or SHA256 repo digests.
  name         = "postgres:15.1"
  # (Boolean) If true, then the Docker image won't be deleted on destroy operation.
  # If this is false, it will delete the image from the docker local storage on destroy operation.
  keep_locally = true
}

locals {
  container_name        = "postgresql"
  container_image       = docker_image.postgresql.name
  container_memory      = 12288
  container_memory_swap = 15360
  container_env         = [
    "POSTGRES_PASSWORD=proaim@2013"
  ]
  container_network = data.terraform_remote_state.network.outputs.network[0]["name"]
  container_ip      = "172.18.0.4"
  container_ports   = [
    {
      internal = 5432
      external = 5432
    }
  ]
  container_volumes = [
    {
      host_path      = "/etc/localtime"
      container_path = "/etc/localtime"
    },
    {
      host_path      = "/data/postgresql/data"
      container_path = "/var/lib/postgresql/data"
    },
  ]
}

# Start a container
resource "docker_container" "postgresql" {
  name            = local.container_name
  image           = local.container_image
  memory          = local.container_memory
  memory_swap     = local.container_memory_swap
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