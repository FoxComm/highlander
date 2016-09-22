'use strict';

module.exports = function(env) {
  const version = 'v1';
  const rootPath = process.env.ROOT_PATH;

  function auth() {
    return {
      header: 'JWT',
      cookieName: 'JWT',
      loginUri: `${rootPath}/login`,
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
