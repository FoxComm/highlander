import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import Home from './components/pages/home';
import Auth from './components/pages/auth';
import Grid from './components/pages/grid';

const routes = (
  <Route path="/" component={Site}>
    <IndexRoute component={Home} />
    <Route path="/login" component={Auth} />
    <Route path="/signup" component={Auth} />
    <Route path="/grid" component={Grid} />
  </Route>
);

export default routes;
