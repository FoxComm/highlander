# Sensu

This role deploys a full [Sensu](https://sensuapp.org) stack, a modern, open source monitoring framework.

## Features
- Deploy a [Sensu](https://sensuapp.org) stack (Redis included in separate base role) and the [Uchiwa dashboard](https://uchiwa.io/#/)
- Integration with the Ansible inventory - deployment of monitoring checks based on inventory grouping
- [Plugins](https://sensuapp.org/plugins) deployment


## Requirements
This role requires:
- Ansible 1.9.x

## Supported Platforms
Tested on:
- Ubuntu 16.04

## Role Variables
Most important default variables:

`sensu_master`: Determines if a node is to act as the Sensu "master" node

`sensu_api_host`: Hostname/IP address of the node running the Sensu API

`redis_host`: Hostname/IP address of the redis node

`sensu_plugins`: A list of plugins to install via `sensu-install` (Ruby Gems)

`sensu_subscriptions`: An Array with client subscriptions


## Example Playbook

Deploy whole Stack:
```
- name: Deploy Sensu Stack
  hosts: sensu-master
  roles:
      - { role: base/redis }
	  - { role: base/sensu, sensu_master: true, redis: redis-server}
```
Deploy Client with [Subscriptions](https://sensuapp.org/docs/latest/reference/clients.html#client-subscriptions)
```
- name: Deploy Sensu Client
  hosts: all
  roles:
	  - { role: base/sensu, sensu_subscriptions ["backend", "frontend", "consul-server"] }
```
Also all clients have a simple `/opt/sensu/embedded/bin/update-client-cfg.rb` script that passing input arguments then  update `client.json` config during terraforming:
```
sudo /opt/sensu/embedded/bin/update-client-cfg.rb $hostname $hostname_ip $sensu_transport_host
```

## Default Plugins
Here a list of plugins which installed by default:
  - [slack](https://github.com/sensu-plugins/sensu-plugins-disk-checks)
  - [cpu-checks](https://github.com/sensu-plugins/sensu-plugins-cpu-checks)
  - [disk-checks](https://github.com/sensu-plugins/sensu-plugins-disk-checks)
  - [io-checks](https://github.com/sensu-plugins/sensu-plugins-io-checks)
  - [memory-checks](https://github.com/sensu-plugins/sensu-plugins-memory-checks)
  - [load-checks](https://github.com/sensu-plugins/sensu-plugins-load-checks)
  - [filesystem-checks](https://github.com/sensu-plugins/sensu-plugins-filesystem-checks)
  - [hardware](https://github.com/sensu-plugins/sensu-plugins-hardware)
  - [network-checks](https://github.com/sensu-plugins/sensu-plugins-network-checks)
  - [uptime-checks](https://github.com/sensu-plugins/sensu-plugins-uptime-checks)
  - [openvpn](https://github.com/sensu-plugins/sensu-plugins-openvpn)
  - [nginx](https://github.com/sensu-plugins/sensu-plugins-nginx)
  - [http](https://github.com/sensu-plugins/sensu-plugins-http)
  - [docker](https://github.com/sensu-plugins/sensu-plugins-docker)
  - [consul](https://github.com/sensu-plugins/sensu-plugins-consul)
  - [mesos](https://github.com/sensu-plugins/sensu-plugins-mesos)
  - [zookeeper](https://github.com/sensu-plugins/sensu-plugins-zookeeper)
  - [elasticsearch](https://github.com/sensu-plugins/sensu-plugins-elasticsearch)

After installation each plugin represented as scripts under `/opt/sensu/embedded/bin/`.
Most of them is self-explained. 
For ex., memory usage:
```
$ /opt/sensu/embedded/bin/check-memory-percent.rb -h

check-memory-percent.sh [ -w value -c value -p -h ]

        -w --> Warning Percentage < value
        -c --> Critical Percentage < value
        -p --> print out performance data
        -h --> print this help screen
```




