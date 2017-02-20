/* @flow */

import React from 'react';
import { Route as ReactRoute, IndexRoute as ReactIndexRoute } from 'react-router';

type RouteProps = {
  component?: ReactClass<*>,
  dimension?: string,
  name: string,
  path?: string,
  frn?: string,
  actions?: string, // what kind of actions are exist in route
};

type FoxRouteProps = RouteProps & {
  routeComponent: ReactRoute|ReactIndexRoute,
}

const FoxRoute = (props: FoxRouteProps) => {
  // assume read action by default
  const { actions = 'r', ...rest } = props;
  const RouteComponent = props.routeComponent;

  return <RouteComponent actions={actions} {...rest} />;
};

export const Route = (props: RouteProps) => {
  return <FoxRoute {...props} routeComponent={ReactRoute} />;
};

export const IndexRoute = (props: RouteProps) => {
  return <FoxRoute {...props} routeComponent={ReactIndexRoute} />
};

