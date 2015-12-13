# -*- mode: ruby -*-
# vi: set ft=ruby :

$cpus = 2
$memory = 2048
$host = "192.168.10.120"

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/vivid64"
  # config.vm.hostname = "ashes"

  config.vm.network :private_network, ip: $host
  config.vm.network :forwarded_port, guest: 80, host: 8181

  config.vm.provider :virtualbox do |vb, override|
    vb.cpus = $cpus
    vb.memory = $memory
  end

  config.vm.provider :vmware_fusion do |v, override|
    override.vm.box_url = "https://s3.eu-central-1.amazonaws.com/ffuenf-vagrantboxes/ubuntu/ubuntu-15.04-server-amd64_vmware.box"
    v.vmx["numvcpus"] = $cpus
    v.vmx["memsize"] = $memory
  end

  config.vm.provision "fix-no-tty", type: "shell" do |s|
    s.privileged = false
    s.inline = "sudo sed -i '/tty/!s/mesg n/tty -s \\&\\& mesg n/' /root/.profile"
  end
  config.vm.provision :shell, path: File.join("ops", "vagrant", "provision.sh")

  config.vm.provider :google do |google, override|
    override.vm.box = "gce"
    google.google_project_id = "foxcomm-staging"
    google.google_client_email = ENV['GOOGLE_CLIENT_EMAIL']
    google.google_json_key_location = ENV['GOOGLE_JSON_KEY_LOCATION']

    google.name = "ashes-stage-02"
    google.image = "ubuntu-1504-vivid-v20150422"
    google.zone = "us-central1-a"

    override.ssh.username = ENV['GOOGLE_SSH_USERNAME']
    override.ssh.private_key_path = ENV['GOOGLE_SSH_KEY']
  end
end
