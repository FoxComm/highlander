/* @flow */

import { request as baseRequest } from './api';

type TArgs = [string, Object];

function searchURI(uri: string): string {
  return `/api/advanced-search/admin/${uri}`;
}

function request(method: string, uri: string, data: Object): Promise<*> {
  const payload = {
    "type": "es",
    "query": {...data},
  };
  return baseRequest(method, searchURI(uri), payload);
}

export function post(...args: TArgs): Promise<*> {
  return request('POST', ...args);
}
