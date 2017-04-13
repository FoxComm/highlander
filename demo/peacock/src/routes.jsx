import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Products from './pages/catalog/products';
import Pdp from './pages/catalog/pdp';
import Search from './pages/search/search';

import Profile from './components/profile/profile';
import ReviewForm from './components/profile/blocks/review-form';

import MensCatPage from './pages/category/men';
import WomensCatPage from './pages/category/women';

import HomePage from './pages/home/home-page';

import Checkout from './pages/checkout/checkout';
import OrderPlaced from './pages/checkout/order-placed/order-placed';

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
        <Route path="reviews/:reviewId" component={ReviewForm} />
        <Route path="/checkout/done" component={OrderPlaced} />
        <Route name="product" path="/products/:productSlug" component={Pdp} />
        <Route name="gift-cards" path="/gift-cards" component={Pdp} />
        <Route name="search" path="/search/:term" component={Search} />
        <Route name="category" path="/s/:categoryName(/:subCategory(/:leafCategory))" component={Products} />
      </Route>
    </Route>
  );
}
