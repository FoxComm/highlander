import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Main from './components/pages/main';
import Products from './components/pages/catalog/products';
import Pdp from './components/pages/catalog/pdp';
import Search from './components/pages/search/search';

import Checkout from './components/pages/checkout/checkout';
import OrderPlaced from './components/pages/checkout/order-placed';

const routes = (
  <Route path="/" component={Site}>
    <Route path="/checkout" component={Checkout} />
    <Route component={StoreFront}>
      <IndexRoute component={Main} />
      <Route path="/all" component={Products} />
      <Route name="category" path=":categoryName" component={Products} />
      <Route name="product" path="/products/:productId" component={Pdp} />
      <Route name="search" path="/search/:term" component={Search} />
      <Route path="/checkout/done" component={OrderPlaced} />
    </Route>
  </Route>
);

export default routes;
