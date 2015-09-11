'use strict';

import React from 'react';
import { Route, DefaultRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Returns from './components/returns/returns';
import Return from './components/returns/return';
import ReturnDetails from './components/returns/details';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import OrderDetails from './components/orders/details';
import Customers from './components/customers/customers';
import Customer from './components/customers/customer';
import Notes from './components/notes/notes';
import Notifications from './components/notifications/notifications';
import ActivityTrail from './components/activity-trail/activity-trail';
import AddressBook from './components/addresses/addresses';
import GiftCards from './components/gift-cards/gift-cards';
import GiftCard from './components/gift-cards/gift-card';
import GiftCardTransactions from './components/gift-cards/transactions';

const routes = (
  <Route handler={Site}>
    <DefaultRoute name="home" handler={Home}/>
    <Route name='orders' handler={Orders}/>
    <Route name='returns' handler={Returns}/>
    <Route name='return' path='/returns/:return' handler={Return}>
      <DefaultRoute name='return-details' handler={ReturnDetails}/>
      <Route name='return-notes' path='notes' handler={Notes}/>
      <Route name='return-notifications' path='notifications' handler={Notifications}/>
      <Route name='return-activity-trail' path='activity-trail' handler={ActivityTrail}/>
    </Route>
    <Route name='order' path='/orders/:order' handler={Order}>
      <DefaultRoute name='order-details' handler={OrderDetails}/>
      <Route name='order-notes' path='notes' handler={Notes}/>
      <Route name='order-returns' path='returns' handler={Returns}/>
      <Route name='order-notifications' path='notifications' handler={Notifications}/>
      <Route name='order-activity-trail' path='activity-trail' handler={ActivityTrail}/>
    </Route>
    <Route name='customers' handler={Customers}/>
    <Route name='customer' path='/customers/:customer' handler={Customer}>
      <Route name='customer-addresses' path='addresses' handler={AddressBook} />
    </Route>
    <Route name='gift-cards' path='/gift-cards' handler={GiftCards} />
    <Route name='giftcard' path='/gift-cards/:giftcard' handler={GiftCard}>
      <DefaultRoute name='gift-card-transactions' handler={GiftCardTransactions} />
      <Route name='gift-card-notes' path='notes' handler={Notes} />
      <Route name='gift-card-activity-trail' path='activity-trail' handler={ActivityTrail} />
    </Route>
  </Route>
);

export default routes;
