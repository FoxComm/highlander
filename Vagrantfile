# -*- mode: ruby -*-
# vi: set ft=ruby :
#
Vagrant.configure("2") do |config|
    config.vm.box = "ubuntu/vivid64"

    config.vm.provider :virtualbox do |vb|
        vb.cpus = 2
        vb.memory = 2048
    end

    config.vm.provider :vmware_fusion do |v, override|
        # TODO: find a better ubuntu/vivid64 for vmware_fusion
        override.vm.box_url = "https://s3.eu-central-1.amazonaws.com/ffuenf-vagrantboxes/ubuntu/ubuntu-15.04-server-amd64_vmware.box"
        v.vmx["memsize"] = 2048
        v.vmx["numvcpus"] = 2
    end

    config.vm.provision :shell, :path => File.join("vagrant", "provision.sh")

    config.vm.network "forwarded_port", guest: 3000, host: 5000
end
