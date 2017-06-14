# Tabernacle

All of our DevOps tools for deploying the application to both development and production.

## Prerequisites

### Required

- [Ansible](https://ansible.com) 2.2.x
- [Docker](https://docker.com) 1.13
- [Go](https://golang.org) 1.6 or above
- [Google Cloud SDK](https://cloud.google.com/sdk/gcloud)
- [jq](https://stedolan.github.io/jq/)
- [Packer](https://packer.io)
- [Python](https://www.python.org) 2.7.x
- [Terraform](https://terraform.io) 0.9.3 or above

### Optional

- [easy](https://github.com/kpashka/easy)

## Ansible Roles Hierarchy

* [`app`](ansible/roles/app) - Roles related to developer appliance launch.
* [`base`](ansible/roles/base) - Roles used during base images packing.
* [`dev`](ansible/roles/dev) - Roles applied over `base` roles during instance runtime.
* [`demo`](ansible/roles/demo) - Everything related to demo/showcase projects.
* [`ext`](ansible/roles/ext) - Customer environment-specific roles.
* [`hotfix`](ansible/roles/hotfix) - Roles that will be merge into `base` roles after repack.
* [`ops`](ansible/roles/ops) - Roles requiring interactive input, usually applied manually.
* [`prod`](ansible/roles/prod) - Roles that are usually applied to production systems.

## Appliance Services Hierarchy

The order, in which `systemd` launches the services:

```
+------------+              +---------+          +------------+
|consul_agent|--------------|zookeeper|----------|mesos_master|
+------------+              +---------+          |mesos_worker|
      |                      |                   +------------+
      |                      |                     |         | 
+--------------------+       |  +-----+            |         | 
|consul_template     |       +--|kafka|--+       +--------+  | 
|demo_consul_template|          +-----+  |       |marathon|  | 
|dashboard           |                   |       +--------+  | 
+--------------------+                   |                   | 
     |                                   |                   | 
     |         +-----+     +---------------+  +------------+ | 
     +---------|nginx|     |schema_registry|  |mesos_consul|-+ 
               +-----+     +---------------+  +------------+   
                                                               
+----------+                                  +---------------+
|postgresql|------+                 +---------|elasticsearch  |
+----------+      |                 |         |elasticsearch_5|
                  |                 |         +---------------+
                  |                 |                          
+----------------------------+   +------+                      
|bottledwater_phoenix        |   |kibana|                      
|bottledwater_middlewarehouse|   +------+                      
|bottledwater_onboarding     |                                 
|materialized_views          |                                 
|pgweb                       |                                 
+----------------------------+                                 
```

## Marathon Groups Hierarchy

The order, in which `highlander` subgroups are launched:

```
 +------------------+      +-------------------+      +-------------------+    +--------------+
 |   core-backend   |      |   core-frontend   |      | core-integrations |    |  ic-storage  |
 |------------------|      |-------------------|      |-------------------|    |--------------|--+
 |* phoenix         |------|* ashes            |------|* hyperion         |    |* henhouse    |  |
 |* isaac           |      |* peacock          |      |* messaging        |    |* neo4j       |  |
 |* solomon         |      |* perfect-gourmet  |      +-------------------+    +--------------+  |
 |* middlewarehouse |      |* top-drawer       |                                                 |
 +------------------+      +-------------------+                                                 |
           |                         |              +---------------------+           +--------------+
           |                         |              |   core-onboarding   |           |   ic-hooks   |
           |                         |              |---------------------|       +---|--------------|
           |                         +--------------|* onboarding-service |       |   |* neo4j-reset |
           |                                        |* onboarding-ui      |       |   +--------------+
           |                                        +---------------------+       |
           |                                                                      |
           |    +------------------------+     +--------------------+          +--------------+
           |    |     core-consumers     |     |    ic-consumers    |          |  ic-backend  |
           |    |------------------------|     |--------------------|          |--------------|
           +----|* green-river           |     |* digger-sphex      |          |* anthill     |
                |* capture-consumer      |     |* orders-anthill    |----------|* bernardo    |
                |* gift-card-consumer    |     |* orders-reviews    |          |* eggcrate    |
                |* shipments-consumer    |     |* orders-sphex      |          |* river-rock  |
                |                        |     |* product-activity  |          +--------------+
                +------------------------+     +--------------------+
```
