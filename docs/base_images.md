# How To Update Base Image

This particular example was being done on `staging` cluster. Different clusters may require different base images to update, e.g. `staging` has only single backend base image, while `vanilla` has 6 backend different machines.

### Preparations
* Cancel all the builds in [Test and Stage Provisioning](https://buildkite.com/foxcommerce/test-and-stage-provisioning) BuildKite project. Ensure they won't run during this process, just cancel them.
* Ensure nobody is running terraform for the same project, it can lead to conflicts. Make an announcement in [#engineering](https://foxcommerce.slack.com/messages/engineering/) channel.
* Connect to staging VPN

### Actions
* Run `packer build -var-file=packer/envs/staging.json packer/tinystack/backend.json`
* Grab base image ID from output, something like `tinystack-backend-1471351876`
* Replace it as a new value for variable `tiny_backend_image` in terraform variables file `terraform/base/gce_dev/dev.tfvars`
* Run terraform planner for staging to ensure everything is ok: `make tf-plan` (it should destroy and re-create 2 machines, for gatling and staging)
* Run actual plan execution: `TF_CMD=apply make tf-stage`
* Commit and push the changes (it will have updated terraform state file) to the master - BuildKite will automatically trigger a new build.

### Source

https://github.com/FoxComm/prov-shit/pull/186#issuecomment-240108581