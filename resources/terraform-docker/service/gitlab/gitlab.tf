# initial password：docker exec -it gitlab grep 'Password:' /etc/gitlab/initial_root_password
# 进入容器配置暴露外部URL：docker exec -it gitlab bash，进入容器后编辑GitLab配置文件：vi /etc/gitlab/gitlab.rb
# 修改：external_url 'http://192.168.100.150:50080'，保存并生效配置：gitlab-ctl reconfigure
# 50022端口拉取代码：git clone ssh://git@192.168.100.150:50022/devops/devops-library.git
# 为了克隆不必麻烦，保留 gitlab 的 22 端口映射，将主机的 sshd 的 22 端口映射到容器中去。将主机的 sshd 端口更改为 50022
# 编辑文件 /etc/ssh/sshd_config，将其中的 #Port 22 注释去掉并设置为50022，重启sshd服务：systemctl restart sshd
# 50080端口拉取代码：git clone http://192.168.100.150:50080/devops/devops-library.git
# 参考文献：https://wangchujiang.com/docker-tutorial/gitlab/index.html

# Pull image
resource "docker_image" "gitlab" {
  # (String) The name of the Docker image, including any tags or SHA256 repo digests.
  name         = "gitlab/gitlab-ce:15.10.1-ce.0"
  # (Boolean) If true, then the Docker image won't be deleted on destroy operation.
  # If this is false, it will delete the image from the docker local storage on destroy operation.
  keep_locally = true
}

locals {
  container_name        = "gitlab"
  container_image       = docker_image.gitlab.name
  container_memory      = 8192
  container_memory_swap = 15360
  container_network     = data.terraform_remote_state.network.outputs.network[0]["name"]
  container_ip          = "172.18.0.2"
  container_ports       = [
    {
      internal = 80
      external = 80
    },
    {
      internal = 443
      external = 443
    },
    {
      internal = 22
      external = 22
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
  restart         = "always"
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