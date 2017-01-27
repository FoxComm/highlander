# Deploying Custom Branches

There are two different approaches of deploying custom container tags to your developer appliance.

[First one](#from-scratch) implies that you don't have a running instance and you'll specify the tags in `.env.local` before running `make up`.

[Second one](#existing-instance) implies that you already have a running instance

Theoretical third way is a smart combination of both ways, just by running `make provision`. We're looking forward to implement it.

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

1. Your appliance IP address. To be replaced by Goldrush statefile.
2. A name of highlander sub-project to re-deploy. Supported values (for now):
    * `ashes`
    * `firebrand`
    * `middlewarehouse`
    * `messaging`
    * `isaac`
    * `solomon`
3. A name of a branch or a tag to be re-deployed.
4. Whether script should (`no` by default) automatically checkout specified branch and try to build, dockerize and push it to repo? Notes:
    * You shouldn't have uncommited files in your current repository state.
    * If you've set `master` branch, build phase will be skipped to avoid accidental overwrites.
