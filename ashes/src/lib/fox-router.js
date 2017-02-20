/* @flow */
import _ from 'lodash';
import React, { Element } from 'react';
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
    const componentName = isIndex ? 'IndexRoute' : 'Route';
    const RouteComponent = isIndex ? IndexRoute : Route;

    if (process.env.PRINT_ROUTES) {
      const jsxParams = {
        ...rest,
      };
      if (frn) {
        jsxParams.frn = frn;
      }
      if (action != 'r') {
        jsxParams.actions = action;
      }

      const nonStrParams = {
        'component': 1, 'frn': 1
      };

      const jsxParamsString = _.toPairs(jsxParams).map(pair => {
        let [name, value] = pair;
        if (name == 'component') {
          value = _.get(value, 'WrappedComponent.name', value.name);
        }

        if (name == 'frn') {
          value = value.replace(/:/g, '.');
        }

        if (name in nonStrParams) {
          value = `{${value}}`;
        } else {
          value = JSON.stringify(value);
        }

        return `${name}=${value}`;
      }).join(' ');

      const content = children ? children.join('\n') : null;

      let result = `<${componentName} ${jsxParamsString}`;
      if (content) {
        result += `>\n${content}\n</${componentName}>`;
      } else {
        result += `/>`;
      }
      return result;
    }


    return (
      <RouteComponent key={`route-${name}`} name={name} {...rest}>
        {children}
      </RouteComponent>
    );
  }
}
