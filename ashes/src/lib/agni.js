/* @flow */

import { request as baseRequest } from './api';

type TArgs = [string, Object];

class Agni {
  searchURI(uri: string): string {
    return `/api/advanced-search/admin/${uri}`;
  }

  request(method: string, uri: string, data: Object): Promise<*> {
    const payload = {
      type: 'es',
      query: {...data},
    };
    return baseRequest(method, this.searchURI(uri), payload);
  }

  search(...args: TArgs): Promise<*> {
    return this.request('POST', ...args);
  }
}

export default new Agni();
