import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';

import FoxRouter from 'lib/fox-router';

import Site from './components/site/site';
import Home from './components/home/home';

import authRoutes from './routes/auth';
import catalogRoutes from './routes/catalog';
import customerRoutes from './routes/customers';
import devRoutes from './routes/dev';
import marketingRoutes from './routes/marketing';
import orderRoutes from './routes/orders';
import settingsRoutes from './routes/settings';

import { getClaims } from 'lib/claims';

const claims = getClaims();

const routes = (
  <Route path="/">
    <IndexRedirect to="/orders/"/>
    {authRoutes(claims)}
    <Route component={Site}>
      <IndexRoute name="home" component={Home}/>
      {orderRoutes(claims)}
      {customerRoutes(claims)}
      {catalogRoutes(claims)}
      {marketingRoutes(claims)}
      {settingsRoutes(claims)}
      {devRoutes(claims)}
    </Route>
  </Route>
);

export default routes;
