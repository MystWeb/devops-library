provider "docker" {
  # Configuration options
  # TCP Connection Docker
  host = "tcp://192.168.100.150:2375"
}

# Data Source：https://developer.hashicorp.com/terraform/language/data-sources
data "terraform_remote_state" "network" {
  backend = "local"
  config = {
    path = "../network/terraform.tfstate"
  }
}