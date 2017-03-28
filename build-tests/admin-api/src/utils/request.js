import superagent from 'superagent';
import makeDebug from 'debug';
import createError from './create-error';

const debug = makeDebug('foxapi');

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
    unauthorizedHandler() {
      if (typeof window != 'undefined') {
        window.location.href = '/login';
      }
    },
    credentials: 'same-origin',
    ...options || {},
    method: method.toUpperCase(),
    headers: {
      ...defaultHeaders,
      ...(options && options.headers || {}),
    },
  };

  const agent = options.agent || superagent;
  const requestPromise =
    agent[method.toLowerCase()](uri)
    .set(options.headers)
    .withCredentials();

  if (data) {
    if (method.toUpperCase() === 'GET') {
      const queryString = serialize(data);

      if (queryString) {
        requestPromise.query(queryString);
      }
    } else {
      requestPromise.send(data);
    }
  }

  debug(`${method.toUpperCase()} ${uri}`);

  if (debug.enabled && data) {
    debug(JSON.stringify(data));
  }

  if (options.handleResponse !== false) {
    const error = new Error();
    Error.captureStackTrace(error, request);
    return requestPromise
      .then(
        response => {
          debug(`${response.status} ${method.toUpperCase()} ${uri}`);
          return response.body;
        },
        err => {
          if (err.statusCode === 401) {
            options.unauthorizedHandler();
          }
          error.message = err.message || String(err);
          error.response = err.response;
          error.responseJson = err.response ? err.response.body : err;
          debug(`${method.toUpperCase()} ${uri} responded with ${err.statusCode}`);
          throw error;
        }
      );
  }

  return requestPromise;
}
