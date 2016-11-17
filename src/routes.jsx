import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/layout/site';
import StoreFront from './components/layout/storefront';
import Products from './pages/catalog/products';
import Pdp from './pages/catalog/pdp';
import Search from './pages/search/search';
import ShippingAndReturns from './pages/static/shipping-and-returns';
import PrivacyPolicy from './pages/static/privacy-policy';
import TermsOfUse from './pages/static/terms-of-use';
import Profile from './components/profile/profile';
import Page from './components/profile/page';
import ProfileUnit from './components/profile/profile-unit';
import EditName from './components/profile/blocks/edit-name';
import EditEmail from './components/profile/blocks/edit-email';
import ChangePassword from './components/profile/blocks/change-password';
import Order from './components/profile/blocks/order';
import StoresPage from './pages/stores/stores-page';
import HomePage from './pages/home/home-page';

import Checkout from './pages/checkout/checkout';
import OrderPlaced from './pages/checkout/04-order-placed/order-placed';

const routes = (
  <Route path="/" component={Site}>
    <Route path="/checkout" component={Checkout} />
    <Route component={StoreFront}>
      <IndexRoute component={HomePage} />
      <Route path="/profile" component={Page}>
        <IndexRoute component={Profile} />
        <Route component={ProfileUnit}>
          <Route path="name" component={EditName} />
          <Route path="email" component={EditEmail} />
          <Route path="password" component={ChangePassword} />
          <Route path="orders/:referenceNumber" component={Order} />
        </Route>
      </Route>
      <Route path="/shipping-and-returns" component={ShippingAndReturns} name="shipping-and-returns" />
      <Route path="/privacy-policy" component={PrivacyPolicy} name="privacy-policy" />
      <Route path="/terms-of-use" component={TermsOfUse} name="terms-of-use" />
      <Route path="/stores" component={StoresPage} name="stores" />
      <Route path="/checkout/done" component={OrderPlaced} />
      <Route path="/products/:productId" component={Pdp} name="product" />
      <Route path="/gift-cards" component={Pdp} name="gift-cards" />
      <Route path="/search/:term" component={Search} name="search" />
      <Route path=":categoryName(/:productType)" component={Products} name="category" />
    </Route>
  </Route>
);

export default routes;
