import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';

import Site from './components/site/site';
import MerchantApplicationPage from './pages/merchant-application/merchant-application-page';
import MerchantAccountPage from './pages/merchant-account/merchant-account-page';
import Main from './pages/container/main';

const routes = (
  <Route path="/" component={Site}>
    <IndexRedirect to="application" />
    <Route component={Main}>
      <IndexRoute component={MerchantApplicationPage} />
      <Route path="application" component={MerchantApplicationPage} />
      <Route path="account" component={MerchantAccountPage} />
    </Route>
  </Route>
);

export default routes;
