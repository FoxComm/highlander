'use strict';

import React from 'react';
import {render} from 'react-dom';
import { renderToString } from 'react-dom/server';
import Router, { RoutingContext, match } from 'react-router';
import {createHistory, createMemoryHistory, createLocation } from 'history';
import routes from './routes';

function createRouteLookupByName(route, prefix = route.props.path) {
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

const app = {

  start() {
    let history = createHistory();
    history.routeLookupByName = createRouteLookupByName(routes);

    render(<Router history={history}>{routes}</Router>, document.getElementById('foxcom'));
  },

  * renderReact(next) {
    const history = createMemoryHistory();

    const location = history.createLocation(this.path);
    history.routeLookupByName = createRouteLookupByName(routes);

    let [redirectLocation, renderProps] = yield match.bind(null, {routes, location, history});

    if (redirectLocation) {
      this.redirect(redirectLocation.pathname + redirectLocation.search);
    } else if (renderProps == null) {
      this.status = 404;
    } else {
      this.state.html = renderToString(<RoutingContext history={history} {...renderProps}/>);
      yield next;
    }
  }
};

export default app;
