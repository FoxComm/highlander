# Firebird

![Firebird and Phoenix](http://i.imgur.com/7Cyj5q8.jpg "Firebird and Phoenix")

### Prerequisites

* node

5.1.0 or above is required version for Firebird.
To install this or anothers versions of node you can use [nvm](https://github.com/creationix/nvm) or [n](https://github.com/tj/n) node version manager.

If using nvm, run the following to install the version listed in `.nvmrc`. For example, if using node `v5.1.0`, follow these commands:

```
nvm install 5.1.0
nvm use 5.1.0
```

### Install npm modules

```
npm install
```

### Run the dev server
```
npm run dev
```

### Push hooks

By default, gulpfile installs pre-push hooks for you.
And, usually, it's ok having pre-push hooks, even if you needed to push broken branch
you can push with `--no-verify` option.
Also, you can disable auto installing hooks by creating `.gulprc` in project root with following content:

```
exports.autoInstallHooks = false;
```
