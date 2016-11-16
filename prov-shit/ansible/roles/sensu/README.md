Role Name
=========

Install and Configure Sensu stack. Based on Ansible Sensu. (http://ansible-sensu.readthedocs.io/en/latest/)

Requirements
------------

Ansible 2.x 

Role Variables
--------------

A description of the settable variables for this role should go here, including any variables that are in defaults/main.yml, vars/main.yml, and any variables that can/should be set via parameters to the role. Any variables that are read from other roles and/or the global scope (ie. hostvars, group vars, etc.) should be mentioned here as well.

Supported Platforms
------------
Ubuntu 16.04


Example Playbook
----------------

  - hosts: sensu-masters
    roles:
      - { role: sensu, sensu_master: true, sensu_include_dashboard: true  }
