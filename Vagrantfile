# -*- mode: ruby -*-
# vi: set ft=ruby ts=2 sw=2 expandtab:
#
require 'fileutils'

CONFIG = File.join(File.dirname(__FILE__), "vagrant.local.rb")

$vb_memory = 6048
$vb_cpu = 4
$backend_ip = "192.168.10.111"
$ashes_ip = "192.168.10.112"
$user = ENV['GOOGLE_SSH_USERNAME'] || "vagrant"

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
end

def expose_backend_ports(config)
    # Kafka
    config.vm.network :forwarded_port, guest: 9092, host: 9092, auto_correct: true

    # Zookeeper
    config.vm.network :forwarded_port, guest: 2181, host: 2181, auto_correct: true

    # Schema Registry
    config.vm.network :forwarded_port, guest: 8081, host: 8081, auto_correct: true

    # PostgreSQL
    config.vm.network :forwarded_port, guest: 5432, host: 5432, auto_correct: true

    # Phoenix
    config.vm.network :forwarded_port, guest: 9090, host: 9090, auto_correct: true

    # ES
    config.vm.network :forwarded_port, guest: 9200, host: 9200, auto_correct: true

    # Kibana
    config.vm.network :forwarded_port, guest: 5601, host: 5601, auto_correct: true

    # Consul
    config.vm.network :forwarded_port, guest: 8500, host: 8500, auto_correct: true
end

def expose_ashes(config)
    config.vm.network :forwarded_port, guest: 80, host: 8282, auto_correct: true
end

Vagrant.configure("2") do |config|
  config.vm.box = "boxcutter/ubuntu1604"

  tune_vm(config, cpus: $vb_cpu, memory: $vb_memory)

  config.vm.provider :vmware_fusion do |v, override|
    override.vm.box = "boxcutter/ubuntu1604"
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

  config.vm.define :appliance, primary: true do |app|
    app.vm.network :private_network, ip: $backend_ip
    expose_backend_ports(app)
    expose_ashes(app)

    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
        ansible.verbose = "vvv"
        ansible.playbook = "ansible/vagrant_appliance.yml"
          ansible.extra_vars = {
              user: $user
          }
    end
  end

  config.vm.define :build, autostart: false do |app|
    app.vm.network :private_network, ip: $backend_ip
    expose_backend_ports(app)
    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
          ansible.verbose = "vvv"
          ansible.skip_tags = "buildkite"
          ansible.playbook = "ansible/vagrant_build_agent.yml"
          ansible.extra_vars = {
              user: $user
          }
      end
  end

  config.vm.define :backend, autostart: false do |app|
    app.vm.network :private_network, ip: $backend_ip
    expose_backend_ports(app)
    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
          ansible.verbose = "vvv"
          ansible.skip_tags = "backup"
          ansible.playbook = "ansible/vagrant_backend.yml"
          ansible.extra_vars = {
              user: $user
          }
      end
  end

  config.vm.define :greenriver, autostart: false do |app|
      backend_host = ENV['BACKEND_HOST'] || $backend_ip
      phoenix_server = ENV['PHOENIX_HOST'] || "#{backend_host}:9090"

      app.vm.network :private_network, ip: $ashes_ip
      expose_ashes(app)

      app.vm.provision "shell", inline: "apt-get install -y python-minimal"
      app.vm.provision "ansible" do |ansible|
          ansible.verbose = "vvv"
          ansible.skip_tags = "backup"
          ansible.playbook = "ansible/vagrant_greenriver.yml"
          ansible.extra_vars = {
              user: $user,
              phoenix_server: phoenix_server,
              greenriver_service_requires: "kafka.service elasticsearch.service",
              greenriver_service_after: "kafka.service elasticsearch.service"
          }
      end
  end

  config.vm.define :ashes, autostart: false do |app|
      backend_host = ENV['BACKEND_HOST'] || $backend_ip
      phoenix_server = ENV['PHOENIX_HOST'] || "#{backend_host}:9090"
      search_server_http = ENV['ES_HOST'] || "#{backend_host}:9200"

      app.vm.network :private_network, ip: $ashes_ip
      expose_ashes(app)
      tune_vm(config, cpus: $ashes_cpu, memory: $ashes_memory)


      app.vm.provision "shell", inline: "apt-get install -y python-minimal"
      app.vm.provision "ansible" do |ansible|
          ansible.verbose = "vv"
          ansible.skip_tags = "backup"
          ansible.playbook = "ansible/vagrant_ashes.yml"
          ansible.extra_vars = {
              user: $user,
              phoenix_server: phoenix_server,
              search_server_http: search_server_http
          }
      end
  end
end
