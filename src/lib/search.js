import fetch from 'isomorphic-fetch';
import _ from 'lodash';

function searchURI(uri) {
  return `/api/search/${uri}`;
}

function serialize(data) {
  const params = [];
  for (let param in data) {
    const value = _.get(data, 'param');
    if (!_.isNull(value)) {
      params.push(encodeURIComponent(param) + '=' + encodeURIComponent(value));
    }
  }
  return params.join('&');
}

function request(method, uri, data) {
  uri = searchURI(uri);

  const headers = {
    'Content-Type': 'application/json:charset=UTF-8'
  };

  const options = { method, headers };

  if (data) {
    if (method.toUpperCase() === 'GET') {
      const queryString = serialize(data);
      if (queryString) {
        uri += `?${queryString}`;
      }
    } else {
      options.body = JSON.stringify(data);
    }
  }

  let error = null;

  return fetch(uri, options)
    .then(response => {
      if (response.status < 200 || response.status >= 300) {
        error = new Error(response.statusText);
        error.response = response;
      }

      return response;
    })
    .then(response => response.text())
    .then(responseText => {
      const json = responseText ? JSON.parse(responseText) : null;

      if (error) {
        error.responseJson = json;
        throw error;
      }

      return json;
    });
}

export function get(...args) {
  return request('GET', ...args);
}

export function post(...args) {
  return request('POST', ...args);
}
