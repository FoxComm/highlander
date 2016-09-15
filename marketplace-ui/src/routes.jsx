import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';

import Site from './components/site/site';
import MerchantApplicationPage from './pages/merchant-application/merchant-application-page';
import Main from './pages/container/main';

const routes = (
  <Route path="/" component={Site}>
    <IndexRedirect to="apply" />
    <Route component={Main}>

      <IndexRoute component={MerchantApplicationPage} />
      <Route path="apply" component={MerchantApplicationPage} />
    </Route>
  </Route>
);

export default routes;
