# Deploying Custom Branches

There are multiple different approaches of deploying custom container tags to your developer appliance:

[First one](#from-scratch) implies that you don't have a running instance and you'll specify the tags in `.env.local` before running `make up`. You will have to build and push this tags manually.

[Second one](#existing-instance) implies that you already have a running instance, a CLI script will use Marathon API to deploy the tags. There is an option to build some of the supported projects automatically.

Theoretical **third way** is a smart combination of both ways, just by running `make provision`, which will handle all possible corner cases. We're looking forward to implement it.

## From scratch

Your generated `.env.local` will have all docker tags set to `master`:

```
export DOCKER_TAG_ASHES=master
export DOCKER_TAG_FIREBRAND=master
export DOCKER_TAG_PHOENIX=master
export DOCKER_TAG_GREENRIVER=master
# ... other project tags ...
```

If you want to spin up an appliance with custom tag for any project, please build and push it to our Docker Registry, for example:

    $ cd phoenix-scala && make build && make docker
    $ DOCKER_TAG=my-custom-debug-build make docker-push

And then set `DOCKER_TAG_PHOENIX=my-custom-debug-build` in your `.env.local`.

After that, when you run `make up`, you'll have a running Phoenix container with specified tag.

## Existing instance

Run this from the root of Highlander:

    $ make update-app

The script will prompt you for various info:

1. A **name of Highlander sub-project** to re-deploy. See list of [supported values](https://github.com/FoxComm/highlander/blob/master/tabernacle/ansible/roles/dev/update_app/vars/main.yml#L8).
2. A **name of a branch / tag** to re-deploy.
3. Whether script should automatically checkout specified branch and try to build, dockerize and push it to repo? Notes:
    * Default value is `no`.
    * If you've set `yes`, you shouldn't have uncommited files in your current repository state.
    * If you've set `master` branch, build phase will be skipped to avoid accidental overwrites.
    * Docker doesn't support slashes in tag names, so `feature/awesome` branch will be converted to `feature-awesome` tag.
4. Optional override of your appliance **IP address**. By default it's being read from `goldrush.state` file, created during `make up`.
