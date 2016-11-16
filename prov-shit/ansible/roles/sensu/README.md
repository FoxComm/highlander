Role Name
=========

Install and Configure Sensu stack. Based on [Ansible Sensu](http://ansible-sensu.readthedocs.io/en/latest/).

Requirements
------------

Ansible 2.x 

Role Variables
--------------



Supported Platforms
------------
Ubuntu 16.04


Example Playbook
----------------

  - hosts: sensu-masters
    roles:
      - { role: sensu, sensu_master: true, sensu_include_dashboard: true  }
