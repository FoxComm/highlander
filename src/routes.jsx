import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import Home from './components/pages/home';
import Auth from './components/pages/auth/auth';
import Login from './components/pages/auth/login';
import SignUp from './components/pages/auth/signup';
import RestorePassword from './components/pages/auth/restore-password';
import ResetPassword from './components/pages/auth/reset-password';
import Grid from './components/pages/grid';

const routes = (
  <Route path="/" component={Site}>
    <IndexRoute component={Home} />
    <Route path="" component={Auth}>
      <Route path="/login" component={Login} />
      <Route path="/signup" component={SignUp} />
      <Route path="/password/restore" component={RestorePassword} />
      <Route path="/password/reset" component={ResetPassword} />
    </Route>
    <Route path="/grid" component={Grid} />
  </Route>
);

export default routes;
