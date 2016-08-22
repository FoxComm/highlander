# -*- mode: ruby -*-
# vi: set ft=ruby ts=2 sw=2 expandtab:
#
require 'fileutils'

CONFIG = File.join(File.dirname(__FILE__), "vagrant.local.rb")

$vb_memory = 1024*6
$vb_cpu = 4
$nginx_ip = "192.168.10.111"
user = "vagrant"

require CONFIG if File.readable?(CONFIG)

def tune_vm(config, opts = {})
  cpus = opts[:cpus]
  memory = opts[:memory]

  config.vm.provider :virtualbox do |vb|
    user = "vagrant"
    vb.cpus = cpus if cpus
    vb.memory = memory if memory
  end

  config.vm.provider :vmware_fusion do |v, override|
    v.vmx["memsize"] = memory if memory
    v.vmx["numvcpus"] = cpus if cpus
  end

  config.vm.provider :google do |g, override|
    user = "ubuntu"

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

  tune_vm(config, cpus: $vb_cpu, memory: $vb_memory)

  config.vm.define :appliance, primary: true do |app|
    app.vm.box = "boxcutter/ubuntu1604"
    app.vm.network :private_network, ip: $nginx_ip

    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
      ansible.verbose = "vvvv"
      ansible.playbook = "prov-shit/ansible/vagrant_appliance.yml"
      ansible.extra_vars = {
        user: user
      }
    end
  end

  config.vm.define :base, autostart: false do |app|
    app.vm.box = "boxcutter/ubuntu1604"
    app.vm.network :private_network, ip: $nginx_ip

    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
      ansible.verbose = "vvvv"
      ansible.skip_tags = "buildkite"
      ansible.playbook = "prov-shit/ansible/vagrant_builder.yml"
      ansible.extra_vars = {
        user: user
      }
    end
  end

  config.vm.define :build, autostart: false do |app|
    app.vm.box = "build16.04"
    app.vm.box_url = "https://s3.amazonaws.com/fc-dev-boxes/build16.04.box"
    app.vm.box_download_checksum = "550f65256533c6dd4bcb5278dfa46ffe"
    app.vm.box_download_checksum_type = "md5"
  end
end
