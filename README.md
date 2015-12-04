# Green River

Green river currently represents consumers that capture [Phoenix](https://github.com/FoxComm/phoenix-scala) data changes and save them to ElasticSearch.

## Running

```
$ sbt -Denv=localhost consume
```

## Vagrant

You can use vagrant to have a ready to run system with all dependencies installed.

```
$ vagrant up
$ vagrant ssh
$ cd /vagrant
$ sbt -Denv=localhost consume
```

## Docs

* [Install](docs/Install.md)
* [Query](docs/Query.md)
