##############################################
# SSH Parameters
##############################################
variable "ssh_user" {}

variable "ssh_private_key" {}

##############################################
# Primary Parameters
##############################################
variable "datacenter" {}

variable "network" {}

variable "amigo_image" {}

variable "logstash_image" {}

variable "database_image" {}

variable "search_image" {}

variable "frontend_image" {}

##############################################
# Secondary Parameters
##############################################
variable "bucket_location" {
  default = "us"
}

variable "dnssimple_domain" {
  default = "foxcommerce.com"
}

variable "zone" {
  default = "us-central1-a"
}

##############################################
# Hardware Configurations
##############################################
variable "amigo_machine_type" {
  default = "n1-standard-2"
}

variable "amigo_disk_size" {
  default = "50"
}

variable "database_machine_type" {
  default = "n1-highmem-8"
}

variable "database_disk_size" {
  default = "500"
}

variable "search_machine_type" {
  default = "n1-highmem-8"
}

variable "search_disk_size" {
  default = "500"
}

variable "logstash_machine_type" {
  default = "n1-standard-2"
}

variable "logstash_disk_size" {
  default = "2000"
}

variable "frontend_machine_type" {
  default = "n1-highcpu-8"
}

variable "frontend_disk_size" {
  default = "250"
}

##############################################
# Storage Buckets
##############################################
resource "google_storage_bucket" "backups" {
  name     = "${var.datacenter}-database-backups"
  location = "${var.bucket_location}"
}

resource "google_storage_bucket" "registry" {
  name     = "${var.datacenter}-docker"
  location = "${var.bucket_location}"
}

##############################################
# Amigo Servers
##############################################
resource "google_compute_instance" "ark-amigo-0" {
  name         = "${var.datacenter}-amigo-0"
  machine_type = "${var.amigo_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.amigo_image}"
    size  = "${var.amigo_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    ]
  }
}

resource "google_compute_instance" "ark-amigo-1" {
  name         = "${var.datacenter}-amigo-1"
  machine_type = "${var.amigo_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.amigo_image}"
    size  = "${var.amigo_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    ]
  }
}

resource "google_compute_instance" "ark-amigo-2" {
  name         = "${var.datacenter}-amigo-2"
  machine_type = "${var.amigo_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.amigo_image}"
    size  = "${var.amigo_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    ]
  }
}

##############################################
# Database Worker
##############################################
resource "google_compute_instance" "ark-database" {
  name         = "${var.datacenter}-database"
  machine_type = "${var.database_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.database_image}"
    size  = "${var.database_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
    ]
  }
}

##############################################
# Search Worker
##############################################
resource "google_compute_instance" "ark-search" {
  name         = "${var.datacenter}-search"
  machine_type = "${var.search_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.search_image}"
    size  = "${var.search_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
    ]
  }
}

##############################################
# Logstash Worker
##############################################
resource "google_compute_instance" "ark-logstash" {
  name         = "${var.datacenter}-logstash"
  machine_type = "${var.logstash_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.logstash_image}"
    size  = "${var.logstash_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
    ]
  }
}

##############################################
# Frontend Worker
##############################################
resource "google_compute_instance" "ark-frontend" {
  name         = "${var.datacenter}-frontend"
  machine_type = "${var.frontend_machine_type}"
  zone         = "${var.zone}"
  tags         = ["http-server", "https-server"]

  disk {
    image = "${var.frontend_image}"
    size  = "${var.frontend_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.ark-amigo-0.network_interface.0.address}",
    ]
  }
}
