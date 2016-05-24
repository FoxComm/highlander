
// WIP. We kinda have to decide if we really want to stick to
// ES6 not ES7 because spread operator is pretty useful here...

import fetch from 'isomorphic-fetch';

export function appendQueryString(url, queryString) {
  if (!queryString) {
    return url;
  }
  const joinWith = url.indexOf('?') != -1 ? '&' : '?';

  return `${url}${joinWith}${queryString}`;
}


function serialize(data) {
  const params = [];
  for (const param in data) {
    if (data.hasOwnProperty(param)) {
      const value = data[param];
      if (value != null) {
        const asString = typeof value != 'string' ? JSON.stringify(value) : value;
        params.push(`${encodeURIComponent(param)}'='${encodeURIComponent(asString)}`);
      }
    }
  }
  return params.join('&');
}


export default function request(method, uri, data, options) {
  const defaultHeaders = {
    'Content-Type': 'application/json;charset=UTF-8',
  };

  options = {
    ...options || {},
    credentials: 'same-origin',
    method: method.toUpperCase(),
    headers: {
      ...defaultHeaders,
      ...(options && options.headers || {}),
    },
  };

  if (data) {
    if (method.toUpperCase() === 'GET') {
      const queryString = serialize(data);
      if (queryString) {
        uri = appendQueryString(uri, queryString);
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
      let json = null;
      if (responseText) {
        try {
          json = JSON.parse(responseText);
        } catch (ex) {
          // invalid json
        }
      }

      if (error) {
        error.responseJson = json;
        throw error;
      }

      return json;
    });
}


