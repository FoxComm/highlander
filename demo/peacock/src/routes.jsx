import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Products from './components/catalog/products';
import Pdp from './components/catalog/pdp';
import Search from './components/search-results/search-results';

import Profile from './components/profile/profile';

import MensCatPage from './components/category/men';
import WomensCatPage from './components/category/women';

import HomePage from './components/home/home-page';

import Checkout from './components/checkout/checkout';
import OrderPlaced from './components/checkout/order-placed/order-placed';

import { isAuthorizedUser } from 'paragons/auth';

export default function makeRoutes(getStore) {
  function handleProfileEnter(nextState, replace, callback) {
    const { auth } = getStore().getState();

    if (!auth || !isAuthorizedUser(auth.user)) {
      replace('/?auth=LOGIN');
    }

    callback();
  }

  return (
    <Route path="/" component={Site}>
      <Route name="checkout" path="/checkout" component={Checkout} />
      <Route component={StoreFront}>
        <IndexRoute component={HomePage} />
        <Route name="profile" path="/profile" component={Profile} onEnter={handleProfileEnter} />
        <Route path="/checkout/done" component={OrderPlaced} />
        <Route path="men" component={MensCatPage} />
        <Route path="women" component={WomensCatPage} />
        <Route name="product" path="/products/:productSlug" component={Pdp} />
        <Route name="gift-cards" path="/gift-cards" component={Pdp} />
        <Route name="search" path="/search/:term" component={Search} />
        <Route name="category" path="/s/:categoryName(/:subCategory(/:leafCategory))" component={Products} />
      </Route>
    </Route>
  );
}
