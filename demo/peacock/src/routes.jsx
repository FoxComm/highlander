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

import HomePage from './pages/home/home-page';

import Checkout from './pages/checkout/checkout';
import OrderPlaced from './pages/checkout/order-placed/order-placed';

import { isAuthorizedUser } from 'paragons/auth';

export default function makeRoutes(store) {
  function handleProfileEnter(nextState, replace, callback) {
    const { auth } = store.getState();

    if (!auth || !isAuthorizedUser(auth.user)) {
      replace('/?auth=LOGIN');
    }

    callback();
  }

  return (
    <Route path="/" component={Site}>
      <Route path="/checkout" component={Checkout} />
      <Route component={StoreFront}>
        <IndexRoute component={HomePage} />
        <Route path="/profile" component={ProfilePage} onEnter={handleProfileEnter}>
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
        <Route path="/products/:productSlug" component={Pdp} name="product" />
        <Route path="/gift-cards" component={Pdp} name="gift-cards" />
        <Route path="/search/:term" component={Search} name="search" />
        <Route path=":categoryName(/:subCategory(/:leafCategory))" component={Products} name="category" />
      </Route>
    </Route>
  );
}
