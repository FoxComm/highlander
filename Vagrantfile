# -*- mode: ruby -*-
# vi: set ft=ruby ts=2 sw=2 expandtab:
#
require 'fileutils'

CONFIG = File.join(File.dirname(__FILE__), "vagrant.local.rb")

$vb_memory = 16348
$vb_cpu = 4
$nginx_ip = "192.168.10.113"
$user = "ubuntu"

require CONFIG if File.readable?(CONFIG)

def tune_vm(config, opts = {})
  cpus = opts[:cpus]
  memory = opts[:memory]

  config.vm.provider :virtualbox do |vb|
    vb.cpus = cpus if cpus
    vb.memory = memory if memory
  end

  config.vm.provider :vmware_fusion do |v, override|
    v.vmx["memsize"] = memory if memory
    v.vmx["numvcpus"] = cpus if cpus
  end

  config.vm.provider :google do |g, override|
    override.vm.box = "gce"
    override.ssh.username = ENV['GOOGLE_SSH_USERNAME']
    override.ssh.private_key_path = ENV['GOOGLE_SSH_KEY']

    g.google_project_id = "foxcomm-staging"
    g.google_client_email = ENV['GOOGLE_CLIENT_EMAIL']
    g.google_json_key_location = ENV['GOOGLE_JSON_KEY_LOCATION']

    g.machine_type = "n1-standard-2"
    g.image = "base-1466535643"
    g.disk_size = 20
    g.zone = "us-central1-a"
    g.tags = ['vagrant', 'no-ports']
  end
end

Vagrant.configure("2") do |config|
  config.vm.box = "boxcutter/ubuntu1604"

  tune_vm(config, cpus: $vb_cpu, memory: $vb_memory)

  config.vm.define :contained, primary: true do |app|
    app.vm.network :private_network, ip: $nginx_ip

    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
      ansible.verbose = "vvvv"
      ansible.playbook = "prov-shit/ansible/vagrant_contained.yml"
      ansible.extra_vars = {
        user: $user
      }
    end
  end
end
