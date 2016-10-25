import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Products from './pages/catalog/products';
import Pdp from './pages/catalog/pdp';
import Search from './pages/search/search';

import Checkout from './pages/checkout/checkout';
import OrderPlaced from './pages/checkout/04-order-placed/order-placed';

const routes = (
  <Route path="/" component={Site}>
    <Route path="/checkout" component={Checkout} />
    <Route component={StoreFront}>
      <IndexRoute component={Products} />
      <Route path="/checkout/done" component={OrderPlaced} />
      <Route path="/products/:productId" component={Pdp} name="product" />
      <Route path=":categoryName(/:productType)" component={Products} name="category" />
      <Route path="/search/:term" component={Search} name="search" />
    </Route>
  </Route>
);

export default routes;
