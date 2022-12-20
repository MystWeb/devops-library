output "network" {
  value = [
    for net in docker_network.network : tomap({
      # ipam_config（Set）Convert（List）
      "name" = net.name, "subnet" : tolist(net.ipam_config)[0].subnet
    })
  ]
}