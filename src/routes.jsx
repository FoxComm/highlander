import React from 'react';
import { Route, IndexRoute } from 'react-router';
import App from './components/layout/app';
import Home from './components/pages/home';
import NoMatch from './components/pages/404';
import Login from './components/pages/login';

const routes = (
  <Route path="/" component={App}>
    <IndexRoute component={Home} />
    <Route path="/login" component={Login} />
    <Route path="*" component={NoMatch} />
  </Route>
);

export default routes;
