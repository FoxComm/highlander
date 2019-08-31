/* @flow */

import { request as baseRequest } from './api';

type TArgs = [string, Object];

function searchURI(uri: string): string {
  return `/api/search/admin/${uri}`;
}

function request(method: string, uri: string, data: Object): Promise<*> {
  return baseRequest(method, searchURI(uri), data);
}

export function get(...args: TArgs): Promise<*> {
  return request('GET', ...args);
}

export function post(...args: TArgs): Promise<*> {
  return request('POST', ...args);
}
