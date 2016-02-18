Build and Deploy for Infrastructure
===================================

Our infrastructure uses the following tools

    * Buildkite for automating build and provisioning execution.
    * Google Compute where our machines live
    * Ansible for provisioning machines.
    * Terraform to setup cloud resources.


Builds
====================================

We have a buildkite pipeline for each product. Each pipeline compiles the project,
runs tests and then deploys the artifacts onto a machine called "stage-buildkite".

The stage-buildkite machines is where our provisioning scripts are executed from
and is the only machine, outside the build machines that is a buildkite agent.

Dumping to stage-buildkite
--------------------------------------
compile -> test -> package -> deploy to stage-buildkite -> trigger "Test & Stage Provision"

The builds are executed on build machines and the artifacts such as jars are put in 
Amazon S3. The exception here is Ashes, where the build simply pulls from git on the stage-buildkite
machine.

Provisioning
======================================

There are two provisioning pipelines at the moment.

    * Test & Stage Provisioning
    * Demo

Test & Stage Provisioning
---------------------------------------

This pipeline runs on the stage-buildkite machine and is executed whenever a project's
build completes successfully. 

All build artifacts are placed in the stage-buildkite using buildkites artifact
store.

We use Ansible for provisioning machines. The pipeline looks like this.

    1. Terraform script is executed which makes sure the VM resources are available.
    2. The Gatling test environment is provisioned and Gatling tests are executed.
    3. Stage environment is provisioned.

If you have VPN access you can access the staging environment at 
[http://stage.foxcommerce.com](http://stage.foxcommerce.com)

Demo
-----------------------------------------

The demo environment pipeline is currently only executed manually. The process
we take to update the environment is the following.

    1. Check staging to make sure everything we want working is working.
    2. Run the Demo buildkite pipeline which will essentially duplicate the staging environment
       but with more data. 

The Demo environment is also available via a public DNS. 
It is protected via a user and password which you can retrieve in the file

    ansible/group_vars/demo-ashes

You can access the demo site here.

[http://demo.foxcommerce.com](http://demo.foxcommerce.com)


FAQ
=======================================

Which user does everything execute in?
-------------------------------------

Everything from builds to provisioning uses a user called "buildkite-agent".

    sudo su buildkite-agent






