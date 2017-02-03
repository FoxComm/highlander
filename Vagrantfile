# -*- mode: ruby -*-
# vi: set ft=ruby ts=2 sw=2 expandtab:
#
require 'fileutils'

CONFIG = File.join(File.dirname(__FILE__), "vagrant.local.rb")

$nginx_ip = "$(hostname -i)"
user = "vagrant"

require CONFIG if File.readable?(CONFIG)

def tune_vm(config, opts = {})
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
    g.image = "appliance-base-170131-101633"
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
  user = ENV['GOOGLE_SSH_USERNAME'] || "vagrant"

  tune_vm(config)

  config.vm.define :appliance, primary: true do |app|
    app.vm.box = "base_appliance_16.04_20161129"
    app.vm.box_url = "https://s3.amazonaws.com/fc-dev-boxes/base_appliance_16.04_20161129.box"
    app.vm.network :private_network, ip: $nginx_ip

    app.vm.provision "ansible" do |ansible|
      ansible.verbose = "v"
      ansible.playbook = "prov-shit/ansible/vagrant_appliance.yml"
      ansible.extra_vars = {
        user: user,
        mesos_ip: $nginx_ip,
        first_run: true,
        docker_tags: {
            ashes: ENV['DOCKER_TAG_ASHES'] || "master",
            firebrand: ENV['DOCKER_TAG_FIREBRAND'] || "master",
            phoenix: ENV['DOCKER_TAG_PHOENIX'] || "master",
            greenriver: ENV['DOCKER_TAG_GREENRIVER'] || "master",
            middlewarehouse: ENV['DOCKER_TAG_MIDDLEWAREHOUSE'] || "master",
            messaging: ENV['DOCKER_TAG_MESSAGING'] || "master",
            isaac: ENV['DOCKER_TAG_ISAAC'] || "master",
            solomon: ENV['DOCKER_TAG_SOLOMON'] || "master",
            capture_consumer: ENV['DOCKER_TAG_CAPTURE_CONSUMER'] || "master",
            gift_card_consumer: ENV['DOCKER_TAG_GIFT_CARD_CONSUMER'] || "master",
            shipments_consumer: ENV['DOCKER_TAG_SHIPMENTS_CONSUMER'] || "master",
            shipstation_consumer: ENV['DOCKER_TAG_SHIPSTATION_CONSUMER'] || "master",
            stock_items_consumer: ENV['DOCKER_TAG_STOCK_ITEMS_CONSUMER'] || "master",
            storefront_topdrawer: ENV['DOCKER_TAG_STOREFRONT_TOPDRAWER'] || "master",
            storefront_tpg: ENV['DOCKER_TAG_STOREFRONT_TPG'] || "master",
            marketplace: ENV['DOCKER_TAG_MARKETPLACE'] || "master",
            marketplace_ui: ENV['DOCKER_TAG_MARKETPLACE_UI'] || "master",
            product_search: ENV['DOCKER_TAG_PRODUCT_SEARCH'] || "master",
            demo_search: ENV['DOCKER_TAG_DEMO_SEARCH'] || "master"
        }
      }
    end
  end
end