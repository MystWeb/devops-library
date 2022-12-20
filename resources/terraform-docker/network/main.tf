provider "docker" {
  # Configuration options
  # TCP Connection Docker
  host = "tcp://192.168.100.150:2375"
}

locals {
  network_settings = [
    {
      name   = "devops"
      driver = "bridge"
      # Docker default bridge Subnet：172.17.0.0/16
      subnet = "172.18.0.0/16"
    }
  ]
}

resource "docker_network" "network" {
  count = length(local.network_settings)
  # (String) The name of the Docker network.
  name = local.network_settings[count.index]["name"]
  # (String) The driver of the Docker network.
  # Possible values are bridge, host, overlay, macvlan. See network docs for more details.
  driver = local.network_settings[count.index]["driver"]
  ipam_config {
    # (String) The subnet in CIDR form
    subnet = local.network_settings[count.index]["subnet"]
  }
}