# -*- mode: ruby -*-
# vi: set ft=ruby :
#
Vagrant.configure("2") do |config|
    config.vm.box = "ubuntu/vivid64"

    config.vm.provider :virtualbox do |vb|
        vb.cpus = 2
        vb.memory = 2048
    end

    config.vm.provision :shell, :path => File.join( "vagrant", "provision.sh" )
end
