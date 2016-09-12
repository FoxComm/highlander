import React from 'react';
import { Route, IndexRoute } from 'react-router';

import Site from './components/site/site';
import Home from './pages/home/home';

const routes = (
  <Route path="/" component={Site}>
    <IndexRoute component={Home}/>

  </Route>
);

export default routes;
