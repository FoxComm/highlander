import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Products from './components/pages/catalog/products';
import Pdp from './components/pages/catalog/pdp';
import Search from './components/pages/search/search';

import Checkout from './components/pages/checkout/checkout';
import OrderPlaced from './components/pages/checkout/order-placed';
import Grid from './components/pages/grid';

const routes = (
  <Route path="/" component={Site}>
    <Route path="/checkout" component={Checkout} />
    <Route path="/grid" component={Grid} />
    <Route component={StoreFront}>
      <Route path="/checkout/done" component={OrderPlaced} />
      <Route name="product" path="/products/:productId" component={Pdp} />
      <IndexRoute component={Products} />
      <Route name="category" path=":categoryName" component={Products} />
      <Route name="search" path="/search/:term" component={Search} />
    </Route>
  </Route>
);

export default routes;
