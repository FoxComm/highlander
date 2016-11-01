# FoxCommerce Project Environments

Navigation:
* [Staging](#staging)
* [Production](#production)
* [Target](#target)

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

## Production

* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcommerce-production-shared&authuser=1)

TBD

## Target

TBD
