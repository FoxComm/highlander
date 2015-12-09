# -*- mode: ruby -*-
# vi: set ft=ruby :
#
Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/vivid64"
  # PostgreSQL
  config.vm.network :forwarded_port, guest: 5432, host: 5432, auto_correct: false

  config.vm.network "private_network", ip: "192.168.10.111"

  config.vm.synced_folder "../phoenix-scala/", "/phoenix"

  config.vm.provision "fix-no-tty", type: "shell" do |s|
      s.privileged = false
      s.inline = "sudo sed -i '/tty/!s/mesg n/tty -s \\&\\& mesg n/' /root/.profile"
  end

  config.vm.provider :virtualbox do |vb|
    vb.cpus = 2
    vb.memory = 3048
  end

  config.vm.provider :vmware_fusion do |v, override|
    override.vm.box_url = "https://s3.eu-central-1.amazonaws.com/ffuenf-vagrantboxes/ubuntu/ubuntu-15.04-server-amd64_vmware.box"
    v.vmx["memsize"] = 3048
    v.vmx["numvcpus"] = 2
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

  config.vm.provision :shell, :path => File.join( "vagrant", "provision.sh" )
end
