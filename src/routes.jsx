import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Auth from './components/pages/auth/auth';
import Login from './components/pages/auth/login';
import SignUp from './components/pages/auth/signup';
import RestorePassword from './components/pages/auth/restore-password';
import ResetPassword from './components/pages/auth/reset-password';
import Products from './components/pages/catalog/products';
import Pdp from './components/pages/catalog/pdp';
import Search from './components/pages/search/search';

import Checkout from './components/pages/checkout/checkout';
import Grid from './components/pages/grid';

const routes = (
  <Route path="/" component={Site}>
    <Route component={Auth}>
      <Route path="/login" component={Login} />
      <Route path="/signup" component={SignUp} />
      <Route path="/password/restore" component={RestorePassword} />
      <Route path="/password/reset" component={ResetPassword} />
    </Route>
    <Route path="/checkout" component={Checkout} />
    <Route path="/grid" component={Grid} />
    <Route component={StoreFront}>
      <Route name="product" path="/products/:productId" component={Pdp} />
      <IndexRoute component={Products} />
      <Route name="category" path=":categoryName" component={Products} />
      <Route name="search" path="/search/:term" component={Search} />
    </Route>
  </Route>
);

export default routes;
