# FoxCommerce Project Environments

Navigation:
* [Staging](#staging)
    * [Development](#development)
* [Production](#production)
    * [The Perfect Gourmet](#the-perfect-gourmet)

## Staging

* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcomm-staging&authuser=1)
* VPN IP: `146.148.43.48`
* Consul: http://dev.consul.foxcommerce.com:8500/ui
* Docker Registry: https://docker-stage.foxcommerce.com:5000
* API Manager: https://console.developers.google.com/apis/credentials?project=foxcomm-staging&authuser=1

### Development

* Mesos: http://10.240.0.24:5050/#/
* Marathon: http://10.240.0.24:8080/ui/#/apps
* Web UI: https://stage.foxcommerce.com

## Production

* Platform: [Google Compute Engine](https://console.cloud.google.com/compute/instances?project=foxcommerce-production-shared&authuser=1)

### The Perfect Gourmet

* SRE: [@kpashka](https://github.com/kpashka)
* VPN IP: `104.198.210.70`
* Docker Registry: https://docker-tpg.foxcommerce.com:5000

#### Staging

* Consul: http://10.0.0.6:8500/ui/
* Kibana: http://10.0.0.6:5601
* Mesos: http://10.0.0.6:5050/#/
* Marathon: http://10.0.0.6:8080/ui/#/apps
* Web UI: https://stage-tpg.foxcommerce.com

#### Production

* Consul: http://docker-tpg.foxcommerce.com:8500/ui/
* Kibana: http://docker-tpg.foxcommerce.com:5601
* Mesos: http://docker-tpg.foxcommerce.com:5050/#/
* Marathon: http://docker-tpg.foxcommerce.com:8080/ui/#/apps
* Web UI: https://tpg.foxcommerce.com
