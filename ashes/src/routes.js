import React from 'react';
import { Route, IndexRoute, Redirect, IndexRedirect } from 'react-router';

import Site from './components/site/site';
import Home from './components/home/home';
import NotFound from './components/not-found/not-found';

import authRoutes from './routes/auth';
import catalogRoutes from './routes/catalog';
import customerRoutes from './routes/customers';
import marketingRoutes from './routes/marketing';
import merchandisingRoutes from './routes/merchandising';
import orderRoutes from './routes/orders';
import settingsRoutes from './routes/settings';

import { getClaims } from 'lib/claims';

const rootPath = process.env.URL_PREFIX;
const indexRedirect = `${rootPath}/orders`;

export default function makeRoutes(jwtToken) {
  const claims = getClaims(jwtToken);

  return [
    <Route path={rootPath} key="root">
      <IndexRedirect to={indexRedirect} />
      {authRoutes(claims)}
      <Route component={Site}>
        <IndexRoute name="home" component={Home} />
        {orderRoutes(claims)}
        {customerRoutes(claims)}
        {catalogRoutes(claims)}
        {marketingRoutes(claims)}
        {merchandisingRoutes(claims)}
        {settingsRoutes(claims)}
      </Route>
      <Route path="*" component={NotFound} />
    </Route>,
    <Redirect from="*" to={indexRedirect} key="index-redirect" />,
  ];
}
