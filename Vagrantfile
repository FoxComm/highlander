# -*- mode: ruby -*-
# vi: set ft=ruby :
#
Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network :forwarded_port, guest: 9090, host: 9090

  config.vm.provider :virtualbox do |vb|
    vb.cpus = 2
    vb.memory = 3048
  end

  config.vm.provider :vmware_fusion do |v, override|
    override.vm.box_url = "https://oss-binaries.phusionpassenger.com/vagrant/boxes/latest/ubuntu-14.04-amd64-vmwarefusion.box"
    v.vmx["memsize"] = 3048
    v.vmx["numvcpus"] = 2
  end

  config.vm.provider :google do |g, override|
    override.vm.box = "gce"
    override.ssh.username = ENV['GOOGLE_SSH_USERNAME']
    override.ssh.private_key_path = ENV['GOOGLE_SSH_KEY']

    g.google_project_id = "foxcomm-stage"
    g.google_client_email = ENV['GOOGLE_CLIENT_EMAIL']
    g.google_json_key_location = ".gce/key.json"

    g.name = "phoenix-stage-01"
    g.machine_type = "n1-standard-2"
    g.image = "ubuntu-1404-trusty-v20150625"
    g.zone = "us-central1-a"
  end

  config.vm.provision :shell, :path => File.join( "vagrant", "provision.sh" )
end
