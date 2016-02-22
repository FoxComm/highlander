import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import Home from './components/pages/home';
import NoMatch from './components/pages/404';
import Login from './components/pages/login';
import Grid from './components/pages/grid';

const routes = (
  <Route path="/" component={Site}>
    <IndexRoute component={Home} />
    <Route path="/login" component={Login} />
    <Route path="/grid" component={Grid} />
    <Route path="*" component={NoMatch} />
  </Route>
);

export default routes;
