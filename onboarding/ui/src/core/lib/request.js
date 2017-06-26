import axios from 'axios';

export default (method, url, data, options = {}) =>
  axios({
    method,
    url,
    data,
    params: method.toUpperCase() === 'GET' ? data : {},
    ...options,
  })
    .then(response => response.data)
    .catch(err => {
      const message = `${method.toUpperCase()} ${url} responded with ${err.response.statusText}`;
      const error = new Error(message);
      error.response = err.response;

      throw error;
    });
