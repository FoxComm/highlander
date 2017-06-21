import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Products from './pages/catalog/products';
import Pdp from './pages/catalog/pdp';
import Search from './pages/search/search';

import ProfilePage from './components/profile/page';
import Profile from './components/profile/profile';
import ProfileUnit from './components/profile/profile-unit';
import EditName from './components/profile/blocks/edit-name';
import EditEmail from './components/profile/blocks/edit-email';
import ChangePassword from './components/profile/blocks/change-password';
import Order from './components/profile/blocks/order';
import AddressForm from './components/profile/blocks/address-form';
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
        <Route name="profile" path="/profile" component={ProfilePage} onEnter={handleProfileEnter}>
          <IndexRoute component={Profile} />
          <Route component={ProfileUnit}>
            <Route path="name" component={EditName} />
            <Route path="email" component={EditEmail} />
            <Route path="password" component={ChangePassword} />
            <Route path="orders/:referenceNumber" component={Order} />
            <Route path="addresses/:addressId" component={AddressForm} />
            <Route path="reviews/:reviewId" component={ReviewForm} />
          </Route>
        </Route>
        <Route path="men" component={MensCatPage} />
        <Route path="women" component={WomensCatPage} />
        <Route path="/checkout/done" component={OrderPlaced} />
        <Route name="product" path="/products/:productSlug" component={Pdp} />
        <Route name="gift-cards" path="/gift-cards" component={Pdp} />
        <Route name="search" path="/search/:term" component={Search} />
        <Route name="category" path="/s/:categoryName(/:subCategory(/:leafCategory))" component={Products} />
      </Route>
    </Route>
  );
}
