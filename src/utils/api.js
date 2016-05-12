

// WIP: moving `lib/api.js` straight over from Firebird because
// it does a lot of good stuff around the mechanics of requests.
// Needs work. We kinda have to decide if we really want to stick to
// ES6 not ES7 because spread operator is pretty useful here...


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


export function request(method, uri, data, options) {
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

  // $FlowFixMe
  let error = null;

  return fetch(uri, options)
    .then(response => {
      if (response.status < 200 || response.status >= 300) {
        error = new Error(response.statusText);
        // $FlowFixMe
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

/* eslint-enable no-param-reassign */


class Api {
  prefix;
  version;
  headers;

  constructor() {}

  addHeaders(headers) {
    this.headers = headers;
    return this;
  }

  addAuth(jwt) {
    this.headers = {
      ...this.headers,
      JWT: jwt,
    };
    return this;
  }

  request(method, uri, data, options = {}) {
    const finalUrl = uri;

    if (this.headers) {
      options.headers = { // eslint-disable-line no-param-reassign
        ...this.headers,
        ...(options.headers || {}),
      };
    }

    return request(method, finalUrl, data, options);
  }

  get(...args) {
    return this.request('get', ...args);
  }

  post(...args) {
    return this.request('post', ...args);
  }

  patch(...args) {
    return this.request('patch', ...args);
  }

  delete(...args) {
    return this.request('delete', ...args);
  }
}

export const api = new Api(args)
