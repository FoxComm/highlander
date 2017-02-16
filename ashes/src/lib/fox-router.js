/* @flow */
import React, { Element} from 'react';
import { Route, IndexRoute } from 'react-router';

import type { Claims } from 'lib/claims';

type FoxRoute = Route|IndexRoute;
type JWT = { claims: Claims };
type RouteParams = {
  component?: ReactClass<*>,
  dimension?: string,
  isIndex?: bool,
  path?: string,
  frn?: string,
};

export default class FoxRouter {
  jwt: JWT;

  constructor(jwt: JWT) {
    this.jwt = jwt;
  }

  create(name: string, params: RouteParams, children?: Array<FoxRoute>): FoxRoute {
    return this.request(name, 'c', params, children);
  }

  read(name: string, params: RouteParams, children?: Array<FoxRoute>): FoxRoute {
    return this.request(name, 'r', params, children);
  }

  update(name: string, params: RouteParams, children?: Array<FoxRoute>): FoxRoute {
    return this.request(name, 'u', params, children);
  }

  request(name: string, action: string, params: RouteParams, children?: Array<FoxRoute>): FoxRoute {
    const { isIndex, frn, ...rest } = params;
    const RouteComponent = isIndex ? IndexRoute : Route;
    return (
      <RouteComponent key={`route-${name}`} name={name} {...rest}>
        {children}
      </RouteComponent>
    );
  }
}
