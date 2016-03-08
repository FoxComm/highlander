
import _ from 'lodash';
import fetch from 'isomorphic-fetch';
import { request as baseRequest } from './api';

function searchURI(uri) {
  return `/api/search/${uri}`;
}

function request(method, uri, data) {
  return baseRequest(method, searchURI(uri), data);
}

export function get(...args) {
  return request('GET', ...args);
}

export function post(...args) {
  return request('POST', ...args);
}
