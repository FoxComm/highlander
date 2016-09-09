# eslint-plugin-no-console-log [![NPM version][npm-image]][npm-url] [![Dependency Status][daviddm-url]][daviddm-image] [![Build Status][travis-image]][travis-url]

An eslint plugin to warn on usage of `console.log`, unlike the built-in rule, this allows other `console` methods.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Install](#install)
- [Configuration](#configuration)
- [Rule Details](#rule-details)
- [When Not To Use It](#when-not-to-use-it)
- [Further Reading](#further-reading)
- [Tests](#tests)
- [Developing](#developing)
  - [Requirements](#requirements)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Install

```sh
npm i -S eslint-plugin-no-console-log
```

## Configuration

Add `plugins` section and specify eslint-plugin-no-console-log as a plugin.

```json
{
  "plugins": [
    "no-console-log"
  ]
}
```

Then, enable the rule.

```json
{
  "rules": {
    "no-console-log/no-console-log": 1
  }
}
```


## Rule Details

This rule warns when it sees `console.log` only. Other variants, like `console.warn` are allowed, as it's assumed you've left them there on purpose. If you'd like to disable all console methods use the built-in rule `no-console`.

The following patterns are considered warnings:

```js

console.log('hi')

```

The following patterns are not warnings:

```js

console.time('timer')
console.timeEnd('timer')
console.warn('oops')
console.error('kittens!')

```

## When Not To Use It

If you want to disable all `console` use to enforce a custom logging option.

## Further Reading

https://github.com/eslint/eslint/issues/2621#issuecomment-105961888

## Tests
Tests are in eslint's [RuleTester](http://eslint.org/docs/user-guide/migrating-to-1.0.0#deprecating-eslint-tester).


* `npm test` will run the tests
* `npm run tdd` will run the tests on every file change.

## Developing
To publish, run `npm run release -- [{patch,minor,major}]`

_NOTE: you might need to `sudo ln -s /usr/local/bin/node /usr/bin/node` to ensure node is in your path for the git hooks to work_

### Requirements
* **npm > 2.0.0** So that passing args to a npm script will work. `npm i -g npm`
* **git > 1.8.3** So that `git push --follow-tags` will work. `brew install git`

## License

Artistic 2.0 © [Joey Baker](http://byjoeybaker.com) and contributors. A copy of the license can be found in the file `LICENSE`.


[npm-url]: https://npmjs.org/package/eslint-plugin-no-console-log
[npm-image]: https://badge.fury.io/js/eslint-plugin-no-console-log.svg
[travis-url]: https://travis-ci.org/joeybaker/eslint-plugin-no-console-log
[travis-image]: https://travis-ci.org/joeybaker/eslint-plugin-no-console-log.svg?branch=master
[daviddm-url]: https://david-dm.org/joeybaker/eslint-plugin-no-console-log.svg?theme=shields.io
[daviddm-image]: https://david-dm.org/joeybaker/eslint-plugin-no-console-log
