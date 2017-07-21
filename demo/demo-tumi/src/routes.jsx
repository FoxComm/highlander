import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from 'components/layout/site';
import StoreFront from 'components/layout/storefront';
import Products from 'pages/catalog/products';
import Pdp from 'pages/catalog/pdp';

import ProfilePage from 'components/profile/page';
import Profile from 'components/profile/profile';
import ProfileUnit from 'components/profile/profile-unit';
import EditName from 'components/profile/blocks/edit-name';
import EditEmail from 'components/profile/blocks/edit-email';
import ChangePassword from 'components/profile/blocks/change-password';
import Order from 'components/profile/blocks/order';
import AddressForm from 'components/profile/blocks/address-form';

import Category from 'pages/catalog/category';
import Collections from 'pages/catalog/collections';

import HomePage from 'pages/home/home-page';

import Checkout from 'pages/checkout/checkout';
import OrderPlaced from 'pages/checkout/04-order-placed/order-placed';

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
        <IndexRoute component={HomePage} fullWidth />
        <Route name="collections" path="/categories/collections" component={Collections} fullWidth />
        <Route name="category-hero" path="/categories/:categoryName" component={Category} fullWidth />
        <Route name="profile" path="/profile" component={ProfilePage} onEnter={handleProfileEnter}>
          <IndexRoute component={Profile} />
          <Route component={ProfileUnit}>
            <Route path="name" component={EditName} />
            <Route path="email" component={EditEmail} />
            <Route path="password" component={ChangePassword} />
            <Route path="orders/:referenceNumber" component={Order} />
            <Route path="addresses/:addressId" component={AddressForm} />
          </Route>
        </Route>
        <Route path="/checkout/done" component={OrderPlaced} />
        <Route name="product" path="/products/:productSlug" component={Pdp} />
        <Route name="gift-cards" path="/gift-cards" component={Pdp} />
        <Route name="search" path="/search" component={Products} />
        <Route name="category" path="/c/:categoryName(/:subCategory(/:leafCategory))" component={Products} />
      </Route>
    </Route>
  );
}
