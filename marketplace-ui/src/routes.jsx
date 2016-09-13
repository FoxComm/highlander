import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';

import Site from './components/site/site';
import Apply from './pages/apply/apply';
import Main from './pages/container/main';

const routes = (
  <Route path="/" component={Site}>
    <IndexRedirect to="apply" />
    <Route component={Main}>

      <IndexRoute component={Apply} />
      <Route path="apply" component={Apply} />
    </Route>
  </Route>
);

export default routes;
