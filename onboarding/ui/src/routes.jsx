import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';

import Site from './components/site/site';
import ApplicationPage from './pages/application/application-page';
import AccountPage from './pages/account/account-page';
import ActionsPage from './pages/actions/actions-page';
import FeedPage from './pages/feed-page/feed-page';
import ShippingPage from './pages/shipping-page/shipping-page';
import ShopifyPage from './pages/shopify-page/shopify-page';
import Main from './pages/container/main';

const routes = (
  <Route path="/" component={Site}>
    <IndexRedirect to="application" />
    <Route path="application" component={Main}>
      <IndexRoute component={ApplicationPage} />
      <Route path=":ref" component={ApplicationPage} />
      <Route path=":ref/account" component={AccountPage} />
      <Route path=":ref/actions" component={ActionsPage} />
    </Route>
  </Route>
);

export default routes;
