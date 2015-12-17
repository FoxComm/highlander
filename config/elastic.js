'use strict';

module.exports = function(env) {
  return {
    uri: process.env.ELASTIC_URL || 'http://10.240.0.7:9200'
  };
};
