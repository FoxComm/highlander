# -*- mode: ruby -*-
# vi: set ft=ruby :
#

Vagrant.configure("2") do |config|
    config.vm.provider :virtualbox do |vb, override|
        override.vm.box = "ubuntu/vivid64"
        vb.cpus = 2
        vb.memory = 2048
    end

    config.vm.provider :vmware_fusion do |v, override|
        # TODO: find a better ubuntu/vivid64 for vmware_fusion
        override.vm.box_url = "https://s3.eu-central-1.amazonaws.com/ffuenf-vagrantboxes/ubuntu/ubuntu-15.04-server-amd64_vmware.box"
        v.vmx["memsize"] = 2048
        v.vmx["numvcpus"] = 2
    end

    config.vm.provision :shell, :path => File.join("ops", "vagrant", "provision.sh")


    config.vm.provider :google do |google, override|
        override.vm.box = "gce"
        google.google_project_id = "foxcomm-stage"
        google.google_client_email = ENV['GOOGLE_CLIENT_EMAIL']
        google.google_json_key_location = ENV['GOOGLE_JSON_KEY_LOCATION']

        google.name = "ashes-stage-02"
        google.image = "ubuntu-1504-vivid-v20150422"
        google.zone = "us-central1-a"

        override.ssh.username = ENV['GOOGLE_SSH_USERNAME']
        override.ssh.private_key_path = ENV['GOOGLE_SSH_KEY']
    end

    config.vm.network "forwarded_port", guest: 80, host: 8181
end
