import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Auth from './components/pages/auth/auth';
import Login from './components/pages/auth/login';
import SignUp from './components/pages/auth/signup';
import RestorePassword from './components/pages/auth/restore-password';
import ResetPassword from './components/pages/auth/reset-password';
import ProductList from './components/pages/catalog/product-list';
import Pdp from './components/pages/catalog/pdp';

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
      <Route name="product" path="/product" component={Pdp} />
      <IndexRoute component={ProductList} />
      <Route name="category" path=":categoryName" component={ProductList} />
    </Route>
  </Route>
);

export default routes;
