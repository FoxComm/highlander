import fetch from './fetch';
import _ from 'lodash';


const isServer = typeof self === 'undefined';

export function appendQueryString(url, queryString) {
  if (!queryString) {
    return url;
  }
  const joinWith = url.indexOf('?') != -1 ? '&' : '?';

  return `${url}${joinWith}${queryString}`;
}

export function addAuthHeaders(headers) {
  const token = localStorage.getItem('jwt');

  if (isServer) {
    const demoToken = process.env.DEMO_AUTH_TOKEN;
    if (demoToken) headers['Authorization'] = `Basic ${demoToken}`;
    return;
  }

  if (token) headers['Authorization'] = token;
}

function serialize(data) {
  if (data.toJSON) data = data.toJSON();

  const params = [];
  for (let param in data) {
    if (data.hasOwnProperty(param)) {
      const value = data[param];
      if (value != null) {
        const asString = _.isObject(value) ? JSON.stringify(value) : value;
        params.push(encodeURIComponent(param) + '=' + encodeURIComponent(asString));
      }
    }
  }
  return params.join('&');
}


export function request(method, uri, data, options = {}) {
  const isFormData = !isServer && data instanceof FormData;

  const headers = {};

  addAuthHeaders(headers);

  if (!isFormData) {
    headers['Content-Type'] = 'application/json;charset=UTF-8';
  }

  options.credentials = "same-origin";
  options.headers = options.headers ? Object.assign(headers, options.headers) : headers;
  options.method = method;

  if (data) {
    if (method.toUpperCase() === 'GET') {
      const queryString = serialize(data);
      if (queryString) {
        uri = appendQueryString(uri, queryString);
      }
    } else {
      options.body = isFormData ? data : JSON.stringify(data);
    }
  }

  let error = null;

  const unauthorizedHandler = options.unauthorizedHandler ? options.unauthorizedHandler : () => {
    window.location.href = "/login";
  };

  return fetch(uri, options)
    .then(response => {
      if (response.status == 401) {
        unauthorizedHandler(response);
      }
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

export default class Api {
  static apiURI(uri) {
    uri = uri.replace(/^\/api\/v\d\/|^\//, '');
    uri = `/api/v1/${uri}`;
    if (isServer) {
      uri = `//api.foxcommerce${uri}`;
    }
    return uri;
  }

  static request(method, uri, data, options = {}) {
    return request(method, this.apiURI(uri), data, options);
  }

  static submitForm(form) {
    const method = form.getAttribute('method');
    const uri = form.getAttribute('action');

    return this.request(method, uri, new FormData(form));
  }

  static get(...args) {
    return this.request('GET', ...args);
  }

  static post(...args) {
    return this.request('POST', ...args);
  }

  static delete(...args) {
    return this.request('DELETE', ...args);
  }

  static put(...args) {
    return this.request('PUT', ...args);
  }

  static patch(...args) {
    return this.request('PATCH', ...args);
  }
}


