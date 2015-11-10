import _ from 'lodash';
import React from 'react';

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

export function transitionTo(history, name, params = {}, query = '') {
  const pathWithParams = interpolateRoute(history, name, params);
  history.pushState(null, pathWithParams, query);
}


export function createRouteLookupByName(route, prefix = route.props.path) {
  let lookup = {};

  React.Children.forEach(route.props.children, (child) => {
    const path = child.type.displayName === 'IndexRoute' ? '' : child.props.path;

    lookup = {...lookup, ...createRouteLookupByName(child, prefix + (prefix.slice(-1) === '/' ? '' : '/') + path)};
  });

  if ((route.type.displayName === 'Route' || route.type.displayName === 'IndexRoute') && route.props.name) {
    lookup[route.props.name] = prefix;
  }

  return lookup;
}

export function addRouteLookupForHistory(createHistory, routes) {
  return function(...args) {
    const history = createHistory(...args);
    history.routeLookupByName = createRouteLookupByName(routes);
    return history;
  };
}
