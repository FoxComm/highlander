# -*- mode: ruby -*-
# vi: set ft=ruby :
#
Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"

  config.vm.provider :virtualbox do |vb|
    vb.cpus = 2
    vb.memory = 2048
  end

  config.vm.provider :vmware_fusion do |v, override|
    override.vm.box_url = "https://oss-binaries.phusionpassenger.com/vagrant/boxes/latest/ubuntu-14.04-amd64-vmwarefusion.box"
    v.vmx["memsize"] = 2048
    v.vmx["numvcpus"] = 2
  end

  config.vm.provision :shell, :path => File.join( "vagrant", "provision.sh" )
end
