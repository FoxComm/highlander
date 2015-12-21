# -*- mode: ruby -*-
# vi: set ft=ruby :
#
require 'fileutils'

CONFIG = File.join(File.dirname(__FILE__), "vagrant.local.rb")

$vb_memory = 4048
$vb_cpu = 2
$vb_host = "192.168.10.111"

require CONFIG if File.readable?(CONFIG)

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/vivid64"
  # PostgreSQL
  config.vm.network :forwarded_port, guest: 5432, host: 5432, auto_correct: true

  # Phoenix
  config.vm.network :forwarded_port, guest: 9090, host: 9090, auto_correct: true

  # ES
  config.vm.network :forwarded_port, guest: 9200, host: 9200, auto_correct: true

  config.vm.network "private_network", ip: $vb_host

  config.vm.provider :virtualbox do |vb|
    vb.cpus = $vb_cpu
    vb.memory = $vb_memory
  end

  config.vm.provider :vmware_fusion do |v, override|
    override.vm.box= "boxcutter/ubuntu1504"
    v.vmx["memsize"] = $vb_memory
    v.vmx["numvcpus"] = $vb_cpu
  end

  config.vm.provider :google do |g, override|
    override.vm.box = "gce"
    override.ssh.username = ENV['GOOGLE_SSH_USERNAME']
    override.ssh.private_key_path = ENV['GOOGLE_SSH_KEY']

    g.google_project_id = "foxcomm-staging"
    g.google_client_email = ENV['GOOGLE_CLIENT_EMAIL']
    g.google_json_key_location = ENV['GOOGLE_JSON_KEY_LOCATION']

    g.name = "green-river-stage-01"
    g.machine_type = "n1-standard-2"
    g.image = "ubuntu-1504-vivid-v20151120"
    g.zone = "us-central1-a"
    g.tags = ['no-ip', 'vagrant']
  end

  config.vm.provision "ansible" do |ansible|
    ansible.verbose = "vv"
    ansible.playbook = "ansible/vagrant.yml"
  end
end
