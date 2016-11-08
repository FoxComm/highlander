# FoxCommerce Project Environments

Navigation:
* [Staging](#staging)
* [Production](#production)
* [Target](#target)

## Staging

* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcomm-staging&authuser=1)
* VPN IP: `146.148.43.48`
* Consul: http://dev.consul.foxcommerce.com:8500/ui
* Docker Registry: https://docker-stage.foxcommerce.com:5000

#### Development

* Mesos: http://10.240.0.24:5050/#/
* Marathon: http://10.240.0.24:8080/ui/#/apps
* Web UI: https://stage.foxcommerce.com

#### The Perfect Gourmet

* Mesos: http://10.240.0.25:5050/#/
* Marathon: http://10.240.0.25:8080/ui/#/apps
* Web UI: https://stage-tpg.foxcommerce.com

#### Target

* Mesos: http://10.240.0.3:5050/#/
* Marathon: http://10.240.0.3:8080/ui/#/apps

#### TopDrawer

* Mesos: http://10.240.0.26:5050/#/
* Marathon: http://10.240.0.26:8080/ui/#/apps

## Production

* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcommerce-production-shared&authuser=1)

#### The Perfect Gourmet

* SRE: [@kpashka](https://github.com/kpashka)
* VPN IP: `104.198.210.70`
* Consul: http://docker-tpg.foxcommerce.com:8500/ui/
* Docker Registry: https://docker-tpg.foxcommerce.com:5000
* Mesos: http://docker-tpg.foxcommerce.com:5050/#/
* Marathon: http://docker-tpg.foxcommerce.com:8080/ui/#/apps
* Web UI: https://tpg.foxcommerce.com

## Target

TBD
