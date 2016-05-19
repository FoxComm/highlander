import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Main from './components/pages/main';
import Products from './components/pages/catalog/products';
import CollectionSummer2016 from './components/pages/collections/summer2016/collection';
import Locations from './components/pages/locations/locations';
import OurStory from './components/pages/our-story/our-story';
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
      <Route path="/collections/summer2016" component={CollectionSummer2016} />
      <Route path="/locations" component={Locations} />
      <Route path="/our-story" component={OurStory} />
      <Route name="category" path=":categoryName" component={Products} />
      <Route name="product" path="/products/:productId" component={Pdp} />
      <Route name="search" path="/search/:term" component={Search} />
      <Route path="/checkout/done" component={OrderPlaced} />
    </Route>
  </Route>
);

export default routes;
