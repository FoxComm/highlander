# Kubernetes

An attempt to launch Fox Platform on [Kubernetes](https://kubernetes.io).

## Pre-requisites

* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl)
* [minikube](https://github.com/kubernetes/minikube)
* [VirtualBox](https://virtualbox.org)

## Running

Start Minikube:

    $ minikube start

Import configuration:

    $ kubectl create -f kubernetes/config.yml
    $ ansible-vault decrypt kubernetes/secrets.yml
    $ kubectl create -f kubernetes/secrets.yml
    $ ansible-vault encrypt kubernetes/secrets.yml

Create Services:

    $ kubectl create -f kubernetes/services.yml
    $ kubectl create -f kubernetes/services_external.yml

Create Deployments:

    # Backend
    $ kubectl create -f kubernetes/deploy_isaac.yml
    $ kubectl create -f kubernetes/deploy_phoenix.yml
    $ kubectl create -f kubernetes/deploy_solomon.yml
    $ kubectl create -f kubernetes/deploy_middlewarehouse.yml
    $ kubectl create -f kubernetes/deploy_hyperion.yml
    $ kubectl create -f kubernetes/deploy_river_rock.yml

    # Frontend
    $ kubectl create -f kubernetes/deploy_ashes.yml
    $ kubectl create -f kubernetes/deploy_peacock.yml

    # Consumers
    $ kubectl create -f kubernetes/deploy_greenriver.yml
    $ kubectl create -f kubernetes/deploy_messaging.yml

## Troubleshooting

* DNS: https://superuser.com/a/722670
* Hosts: https://github.com/kubernetes/minikube/issues/352#issuecomment-237615642
* Volumes: https://github.com/razius/minikube/pull/1/files

## Snippets

DNS cluster addon test:

    $ kubectl run curl --image=radial/busyboxplus:curl -i --tty --restart=Never
    $ nslookup peacock-lb
    $ kubectl delete po/curl

