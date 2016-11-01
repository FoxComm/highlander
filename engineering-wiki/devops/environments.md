# FoxCommerce Project Environments

Navigation:
* [Staging](#staging)
* [TopDrawer](#topdrawer)
* [Target](#target)
* [The Perfect Gourmet](#the-perfect-gourmet)

## Staging

* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcomm-staging&authuser=1)
* VPN IP: `146.148.43.48`
* Docker Registry: https://docker.stage.foxcommerce.com:5000 (uses `stage-docker` storage bucket)

| Name       | Link                                                             |
|:-----------|:-----------------------------------------------------------------|
| Consul     | [Consul UI](http://10.240.0.24:8500/ui/#/foxcomm-stage/services) |
| Mesos      | [Mesos UI](http://10.240.0.24:5050/#/)                           |
| Marathon   | [Marathon UI](http://10.240.0.24:8080/ui/#/apps)                 |
| Storefront | [Storefront](https://stage.foxcommerce.com)                      |
| Admin UI   | [Admin UI](https://stage.foxcommerce.com/admin)                  |

## TopDrawer

* SRE - [@mempko](https://github.com/mempko)
* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcommerce-production-shared&authuser=1)
* VPN IP: `130.211.158.41`
* Docker Registry: https://docker.topdrawer.foxcommerce.com:5000 (uses `topdrawer-docker` storage bucket)

| Name       | Staging                                                          | Vanilla                                                        |
|:-----------|:-----------------------------------------------------------------|:---------------------------------------------------------------|
| Consul     | [Consul UI](http://10.0.0.15:8500/ui/#/topdrawer-stage/services) | [Consul UI](http://10.0.0.3:8500/ui/#/topdrawer/services)      |
| Mesos      | [Mesos UI](http://10.0.0.15:5050/#/)                             | [Mesos UI](http://10.0.0.3:5050/#/)                            |
| Marathon   | [Marathon UI](http://10.0.0.15:8080/ui/#/apps)                   | [Marathon UI](http://10.0.0.3:8080/ui/#/apps)                  |
| Storefront | [Storefront](https://topdrawer-stage.foxcommerce.com)            | [Storefront](https://topdrawer-production.foxcommerce.com)     |
| Admin UI   | [Admin UI](https://admin.topdrawer-stage.foxcommerce.com)        | [Admin UI](https://admin.topdrawer-production.foxcommerce.com) |

## Target

* SRE - [@kpashka](https://github.com/kpashka)
* Platform: [Amazon Web Services](https://us-west-2.console.aws.amazon.com/ec2/v2/home?region=us-west-2#Instances:sort=instanceId)
* VPN IP: `52.89.219.60` (use `tun` device, instead of `tap`)

| Name            | Staging                                                                              | Vanilla                                                                          |
|:----------------|:-------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------|
| Consul          | [Consul UI](http://docker-tgt-stage.foxcommerce.com:8500/ui/#/target-stage/services) | [Consul UI](http://docker-tgt-vanilla.foxcommerce.com:8500/ui/#/target/services) |
| Mesos           | [Mesos UI](http://docker-tgt-stage.foxcommerce.com:5050/#/)                          | [Mesos UI](http://10.0.7.209:5050/#/)                                            |
| Marathon        | [Marathon UI](http://docker-tgt-stage.foxcommerce.com:8080/ui/#/apps)                | [Marathon UI](http://docker-tgt-vanilla.foxcommerce.com:8080/ui/#/apps)          |
| Docker Registry | [Catalog](https://docker-tgt-stage.foxcommerce.com:5000/v2/_catalog)                 | [Catalog](https://docker-tgt-vanilla.foxcommerce.com:5000/v2/_catalog)           |
| S3 Bucket       | `s3-docker-stage`                                                                    | `s3-docker-vanilla`                                                              |
| Storefront      | [Storefront](https://tgt-stage.foxcommerce.com)                                      | [Storefront](https://tgt-vanilla.foxcommerce.com) (TBD)                          |
| Admin UI        | [Admin UI](https://admin-tgt-stage.foxcommerce.com/admin/login)                      | [Admin UI](https://admin-tgt-vanilla.foxcommerce.com/admin/login) (TBD)          |

## The Perfect Gourmet

* SRE - [@alexkreskiyan](https://github.com/alexkreskiyan)
* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcommerce-production-shared&authuser=1)
* VPN IP: `104.198.210.70`
* Docker Registry: TBD (uses `tpg-docker` storage bucket)

| Name            | Staging                                                   | Vanilla                                              |
|:----------------|:----------------------------------------------------------|:-----------------------------------------------------|
| Consul          | [Consul UI](http://10.0.0.3:8500/ui/#/tpg-stage/services) | [Consul UI](http://10.0.0.10:8500/ui/#/tpg/services) |
| Mesos           | TBD                                                       | TBD                                                  |
| Marathon        | [Marathon UI](http://10.0.0.3:8080/ui/#/apps)             | [Marathon UI](http://10.0.0.10:8080/ui/#/apps)       |
| Storefront      | TBD                                                       | TBD                                                  |
| Admin UI        | TBD                                                       | TBD                                                  |
