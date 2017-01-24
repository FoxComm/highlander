# -*- mode: ruby -*-
# vi: set ft=ruby ts=2 sw=2 expandtab:
#
require 'fileutils'

CONFIG = File.join(File.dirname(__FILE__), "vagrant.local.rb")

$vb_memory = 1024*8
$vb_cpu = 4
$nginx_ip = "192.168.10.111"
$local = true
user = "vagrant"

require CONFIG if File.readable?(CONFIG)

def expose_ports(config)
    # Mesos
    config.vm.network :forwarded_port, guest: 5050, host: 5050, auto_correct: true

    # Marathon
    config.vm.network :forwarded_port, guest: 8080, host: 8080, auto_correct: true

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

    # Middlewarehouse
    config.vm.network :forwarded_port, guest: 9292, host: 9292, auto_correct: true

    #Nginx
    config.vm.network :forwarded_port, guest: 80, host: 80, auto_correct: true

    #Nginx https
    config.vm.network :forwarded_port, guest: 443, host: 443, auto_correct: true
end

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
    override.vm.synced_folder '.', '/vagrant', disabled: true

    override.ssh.username = ENV['GOOGLE_SSH_USERNAME']
    override.ssh.private_key_path = ENV['GOOGLE_SSH_KEY']

    g.google_project_id = "foxcomm-staging"
    g.google_client_email = ENV['GOOGLE_CLIENT_EMAIL']
    g.google_json_key_location = ENV['GOOGLE_JSON_KEY_LOCATION']

    g.machine_type = "n1-standard-4"
    g.image = "appliance-base-170123-162047"
    g.disk_size = 40
    g.zone = "us-central1-a"
    g.tags = ['vagrant', 'no-ports']

    if ENV['GOOGLE_INSTANCE_NAME']
      g.name = ENV['GOOGLE_INSTANCE_NAME']
    end
  end

  config.vm.provider :aws do |aws, override|
    override.vm.box = "dummy"
    override.ssh.username = "ubuntu"

    aws.access_key_id = ENV['AWS_ACCESS_KEY_ID']
    aws.secret_access_key = ENV['AWS_SECRET_ACCESS_KEY']
    aws.keypair_name = ENV['AWS_KEY_NAME']
    aws.associate_public_ip = true
    aws.elastic_ip = "52.38.159.101"
    aws.block_device_mapping = [{ 'DeviceName' => '/dev/sda1', 'Ebs.VolumeSize' => 100 }]

    aws.ami = "ami-191fd379"
    aws.region="us-west-2"
    aws.instance_type = "m4.xlarge"
    aws.subnet_id = ENV['AWS_SUBNET']
    aws.security_groups = [ENV['AWS_SG_GROUP']]
    override.ssh.private_key_path = ENV['AWS_KEY_PATH']
  end
end

Vagrant.configure("2") do |config|
  $master = "master"
  user = ENV['GOOGLE_SSH_USERNAME'] || "vagrant"

  tune_vm(config, cpus: $vb_cpu, memory: $vb_memory)

  config.vm.define :appliance, primary: true do |app|
    app.vm.box = "base_appliance_16.04_20161129"
    app.vm.box_url = "https://s3.amazonaws.com/fc-dev-boxes/base_appliance_16.04_20161129.box"

    app.vm.network :private_network, ip: $nginx_ip
    expose_ports(app)

    # Workaround for mitchellh/vagrant#1867
    if ARGV[1] and \
       (ARGV[1].split('=')[0] == "--provider" or ARGV[2])
      provider = (ARGV[1].split('=')[1] || ARGV[2]).chomp
    else
      provider = (ENV['VAGRANT_DEFAULT_PROVIDER'] || "virtualbox").chomp
    end

    if provider == "google"
      puts 'Overriding Google-specific variables'
      $nginx_ip = "`hostname -I | awk '{print $1}'`"
      $local = false
    end

    app.vm.provision "ansible" do |ansible|

      ansible.verbose = "v"
      ansible.playbook = "prov-shit/ansible/vagrant_appliance.yml"
      ansible.extra_vars = {
        user: user,
        appliance_hostname: $nginx_ip,
        mesos_ip: $nginx_ip,
        local_vagrant: $local,
        first_run: true,
        docker_tag_ashes: ENV['DOCKER_TAG_ASHES'] || $master,
        docker_tag_firebrand: ENV['DOCKER_TAG_FIREBRAND'] || $master,
        docker_tag_phoenix: ENV['DOCKER_TAG_PHOENIX'] || $master,
        docker_tag_greenriver: ENV['DOCKER_TAG_GREENRIVER'] || $master,
        docker_tag_middlewarehouse: ENV['DOCKER_TAG_MIDDLEWAREHOUSE'] || $master,
        docker_tag_messaging: ENV['DOCKER_TAG_MESSAGING'] || $master,
        docker_tag_isaac: ENV['DOCKER_TAG_ISAAC'] || $master,
        docker_tag_solomon: ENV['DOCKER_TAG_SOLOMON'] || $master,
        docker_tag_capture_consumer: ENV['DOCKER_TAG_CAPTURE_CONSUMER'] || $master,
        docker_tag_gift_card_consumer: ENV['DOCKER_TAG_GIFT_CARD_CONSUMER'] || $master,
        docker_tag_shipments_consumer: ENV['DOCKER_TAG_SHIPMENTS_CONSUMER'] || $master,
        docker_tag_shipstation_consumer: ENV['DOCKER_TAG_SHIPSTATION_CONSUMER'] || $master,
        docker_tag_stock_items_consumer: ENV['DOCKER_TAG_STOCK_ITEMS_CONSUMER'] || $master,
        docker_tag_storefront_topdrawer: ENV['DOCKER_TAG_STOREFRONT_TOPDRAWER'] || $master,
        docker_tag_storefront_tpg: ENV['DOCKER_TAG_STOREFRONT_TPG'] || $master,
        docker_tag_marketplace: ENV['DOCKER_TAG_MARKETPLACE'] || $master,
        docker_tag_marketplace_ui: ENV['DOCKER_TAG_MARKETPLACE_UI'] || $master,
        docker_tag_product_search: ENV['DOCKER_TAG_PRODUCT_SEARCH'] || $master,
        docker_tag_demo_search: ENV['DOCKER_TAG_DEMO_SEARCH'] || $master
      }
    end
  end

  config.vm.define :build, autostart: false do |app|
    app.vm.box = "build16.04"
    app.vm.box_url = "https://s3.amazonaws.com/fc-dev-boxes/build16.04.box"
    app.vm.box_download_checksum = "550f65256533c6dd4bcb5278dfa46ffe"
    app.vm.box_download_checksum_type = "md5"
  end

  config.vm.define :appliance_base, autostart: false do |app|
    app.vm.box = "boxcutter/ubuntu1604"
    app.vm.network :private_network, ip: $nginx_ip

    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
      ansible.verbose = "v"
      ansible.playbook = "prov-shit/ansible/vagrant_appliance_base.yml"
      ansible.extra_vars = {
        user: user
      }
    end
  end

  config.vm.define :build_base, autostart: false do |app|
    app.vm.box = "boxcutter/ubuntu1604"
    app.vm.network :private_network, ip: $nginx_ip

    app.vm.provision "shell", inline: "apt-get install -y python-minimal"
    app.vm.provision "ansible" do |ansible|
      ansible.verbose = "v"
      ansible.skip_tags = "buildkite"
      ansible.playbook = "prov-shit/ansible/vagrant_builder.yml"
      ansible.extra_vars = {
        user: user
      }
    end
  end
end
