'use strict';

module.exports = function(env) {
  return {
    uri: process.env.ELASTIC_URL || 'http://localhost:9200'
  };
};
