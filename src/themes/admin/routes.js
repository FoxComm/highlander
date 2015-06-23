'use strict';

import React from 'react';
import { Route, DefaultRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import OrderDetails from './components/orders/details';
import Customers from './components/customers/customers';
import Customer from './components/customers/customer';
import Notes from './components/notes/notes';

import AddressBook from './components/addresses/addresses';

const routes = (
  <Route handler={Site}>
    <DefaultRoute name="home" handler={Home}/>
    <Route name='orders' handler={Orders}/>
    <Route name='order' path='/orders/:order' handler={Order}>
      <DefaultRoute name='order-details' handler={OrderDetails}/>
      <Route name='order-notes' path='notes' handler={Notes}/>
    </Route>
    <Route name='customers' handler={Customers}/>
    <Route name='customer' path='/customers/:customer' handler={Customer}>
      <Route name='customer-addresses' path='addresses' handler={AddressBook} />
    </Route>
  </Route>
);

export default routes;
