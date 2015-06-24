'use strict';

export default class Api {
  static apiURI(uri) {
    uri = uri.replace(/^\/api\/v\d\/|^\//, '');
    return `/api/v1/${uri}`;
  }

  static request(method, uri, data) {
    uri = this.apiURI(uri);
    return new Promise((resolve, reject) => {
      let
        req   = new XMLHttpRequest(),
        token = localStorage.getItem('token');
      req.onload = function() {
        if (req.status >= 200 && req.status < 300) {
          resolve(JSON.parse(req.response));
        } else {
          try {
            reject(JSON.parse(req.response));
          } catch(err) {
            reject(err);
          }
        }
      };

      req.open(method, uri);
      if (token) req.setRequestHeader('Authorization', `Bearer ${token}`);
      if (data && !data instanceof FormData) {
        req.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
        data = JSON.stringify(data);
      }
      req.send(data);
    });
  }

  static submitForm(form) {
    let
      method    = form.getAttribute('method').toLowerCase(),
      uri       = form.getAttribute('action'),
      formData  = new FormData(form);
    return this[method](uri, formData);
  }

  static get() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['GET'].concat(args));
  }

  static post() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['POST'].concat(args));
  }

  static delete() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['DELETE'].concat(args));
  }

  static put() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['PUT'].concat(args));
  }
}
