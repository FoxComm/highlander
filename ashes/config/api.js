'use strict';

module.exports = function(env) {
  const version = 'v1';
  const onServer = process.env.ON_SERVER;
  const loginUri = onServer ? '/admin/login' : '/login';

  function auth() {
    return {
      header: 'JWT',
      cookieName: 'JWT',
      loginUri: loginUri,
      publicKey: env.public_key,
    };

  }

  return {
    host: process.env.API_URL,
    uri: `${process.env.API_URL}/${version}`,
    auth: auth(),
    version: version
  };
};
