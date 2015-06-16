'use strict';

import React from 'react';
import { Route, DefaultRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import Users from './components/users/users';
import User from './components/users/user';

import Countdown from './components/countdown/countdown';

const routes = (
  <Route handler={Site}>
    <DefaultRoute name="home" handler={Home}/>
    <Route name='orders' handler={Orders}>
      <Route name='order' path=':order' handler={Order}/>
    </Route>
    <Route name='countdown' path='/countdown' handler={Countdown}/>
    <Route name='users' handler={Users}>
      <Route name='user' path=':user' handler={User}/>
    </Route>
  </Route>
);

export default routes;
