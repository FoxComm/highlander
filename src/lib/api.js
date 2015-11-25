import fetch from 'isomorphic-fetch';

const isServer = typeof self === 'undefined';

export default class Api {
  static apiURI(uri) {
    uri = uri.replace(/^\/api\/v\d\/|^\//, '');
    uri = `/api/v1/${uri}`;
    if (isServer) {
      uri = `//api.foxcommerce${uri}`;
    }
    return uri;
  }

  static serialize = function(data) {
    const params = [];
    for (let param in data) {
      if (data.hasOwnProperty(param)) {
        const value = data[param];
        if (value != null) {
          params.push(encodeURIComponent(param) + '=' + encodeURIComponent(value));
        }
      }
    }
    return params.join('&');
  };

  static request(method, uri, data) {
    uri = this.apiURI(uri);

    const isFormData = !isServer && data instanceof FormData;
    const token = localStorage.getItem('token');
    const headers = {};

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    if (!isFormData) {
      headers['Content-Type'] = 'application/json;charset=UTF-8';
    }

    const options = {
      method,
      headers
    };

    if (data) {
      if (method.toUpperCase() === 'GET') {
        const queryString = this.serialize(data);
        if (queryString) {
          uri += `?${queryString}`;
        }
      } else {
        options.body = isFormData ? data : JSON.stringify(data);
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
