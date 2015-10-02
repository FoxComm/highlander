'use strict';

import _ from 'lodash';

export function interpolateRoute(history, name, params = {}) {
  let path = history.routeLookupByName[name];

  if (!path) {
    console.warn('Route name not found:', name);
    return;
  }

  return _.reduce(params, (path, paramV, paramK) => {
    if (path.indexOf(`:${paramK}`) === -1) {
      console.warn(`Route param ${paramK} not found in route name ${name}.`);
    }

    return path.replace(`:${paramK}`, paramV);
  }, path);
}

export function transitionTo(history, name, params = {}, query = '')  {
  let pathWithParams = interpolateRoute(history, name, params);
  history.pushState(null, pathWithParams, query);
}
