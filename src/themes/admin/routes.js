'use strict';

import React from 'react';
import { Route, DefaultRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';

const routes = (
  <Route handler={Site}>
    <DefaultRoute name="home" handler={Home}/>
  </Route>
);

export default routes;
