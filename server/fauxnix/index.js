'use strict';

const
  fs      = require('fs'),
  Router  = require('koa-router');

module.exports = function(app) {
  const config  = app.config.api;
  let router = new Router({
    prefix: `/fauxnix/${config.version}`
  });

  router.use(app.jsonError);

  for (let file of fs.readdirSync(`${__dirname}/controllers`)) {
    require(`${__dirname}/controllers/${file}`)(app, router);
  }

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
