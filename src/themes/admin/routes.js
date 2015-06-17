'use strict';

import React from 'react';
import { Route, DefaultRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import Customers from './components/customers/customers';
import Customer from './components/customers/customer';
import Notes from './components/notes/notes';

import AddressBook from './components/addresses/addresses';

const routes = (
  <Route handler={Site}>
    <DefaultRoute name="home" handler={Home}/>
    <Route name='addresses' handler={AddressBook} />
    <Route name='orders' handler={Orders}/>
    <Route name='order' path='/orders/:order' handler={Order}>
      <Route name='notes' path='notes' handler={Notes}/>
    </Route>
    <Route name='customers' handler={Customers}>
      <Route name='customer' path=':customer' handler={Customer}/>
    </Route>
  </Route>
);

export default routes;
