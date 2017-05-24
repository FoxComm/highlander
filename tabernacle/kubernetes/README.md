# Kubernetes

An attempt to launch Fox Platform on [Kubernetes](https://kubernetes.io).

## Pre-requisites

* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl)
* [minikube](https://github.com/kubernetes/minikube)
* [VirtualBox](https://virtualbox.org)

## Running

Start Minikube:

    $ minikube start

Import [configuration](https://kubernetes.io/docs/tasks/configure-pod-container/configmap/):

    $ kubectl create -f kubernetes/config.yml

Decrypt and import [secrets](https://kubernetes.io/docs/concepts/configuration/secret/):

    $ ansible-vault decrypt kubernetes/secrets.yml
    $ kubectl create -f kubernetes/deploy_ashes.yml

Create [deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment):

    # Backend
    $ kubectl create -f kubernetes/deploy_isaac.yml
    $ kubectl create -f kubernetes/deploy_solomon.yml
    $ kubectl create -f kubernetes/deploy_middlewarehouse.yml
    $ kubectl create -f kubernetes/deploy_hyperion.yml

    # Frontend
    $ kubectl create -f kubernetes/deploy_ashes.yml
    $ kubectl create -f kubernetes/deploy_peacock.yml

    # Consumers
    $ kubectl create -f kubernetes/deploy_greenriver.yml
    $ kubectl create -f kubernetes/deploy_messaging.yml

## Snippets

DNS cluster addon test:

    $ kubectl run curl --image=radial/busyboxplus:curl -i --tty
    $ nslookup peacock-lb
