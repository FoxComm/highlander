/* @flow */

import React, { Component, Element } from 'react';
import { Route, IndexRoute } from 'react-router';
import _ from 'lodash';

import FoxRoute from '../common/components/route';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import CartsListPage from 'components/carts/list-page';
import Carts from 'components/carts/carts';
import Cart from 'components/carts/cart';
import CartDetails from 'components/carts/details';
import Notes from 'components/notes/notes';
import NewOrder from 'components/orders/new-order';
import OrdersListPage from 'components/orders/list-page';
import Orders from 'components/orders/orders';
import Order from 'components/orders/order';
import OrderDetails from 'components/orders/details';
import Shipments from 'components/orders/shipments/shipments';

import type { Claims } from 'lib/claims';

const orderClaims: Claims = {
  'frn:oms:order': ['c', 'r', 'u', 'd'],
};

const cartClaims: Claims = {
  'frn:oms:cart': ['c', 'r', 'u', 'd'],
};

class Routerr {
  constructor(jwt) {
    this.jwt = jwt;
  }

  route(params, children) {
    console.log('Hello');
  }
}

const getRoutes = (jwt: Object) => {
  let route = Routerr(jwt);

  const readCart = { 'frn:oms:cart': ['r'] };
  const readOrder = { 'frn:oms:order': ['r'] };
  const createOrder = { 'frn:oms:order': ['c', 'r'] };

  const cartFRN = 'frn:oms:cart';
  const orderFRN = 'frn:oms:order';
  const activityFRN = 'frn:oms:order-activity';
  const noteFRN = 'frn:oms:order-note';
  const shipmentFRN = 'frn:oms:order-shipment';

  const cartRoutes =
    route.read('carts-base', { path: 'carts', frn: cartFRN }, [
      route.read('carts-list-pages', { component: CartsListPage }, [
        route.read('carts', { component: Carts, isIndex: true })
      ]),
      route.read('cart', { path: ':cart', component: Cart}, [
        route.read('cart-details', { component: CartDetails, isIndex: true}),
        route.read('cart-notes', { path: 'notes', component: Notes}),
        route.read('cart-activity-trail', { path: 'activity-trail', component: ActivityTrailPage}),
      ]),
    ]);

  const orderRoutes =
    route.read({ name: 'orders-base', path: 'orders', frn: orderFRN }, [
      route.create({ name: 'new-order', path: 'new', component: NewOrder }),
      route.read({ name: 'orders-list-pages', component: OrdersListPage }, [
        route.read({ 'orders', component: Orders, isIndex: true }),
        route.read({ name: 'orders-activity-trail', path: 'activity-trail', component: ActivityTrailPage, frn: activityFRN }),
      ]),
      route.read({ name: 'order', path: ':order', component: Order }, [
        route.read({ name: 'order-details', component: OrderDetails, isIndex: true }),
        route.read({ name: 'order-shipments', path: 'shipments', component: Shipments, frn: shipmentFRN }),
        route.read({
          name: 'order-shipments',
          path: 'shipments',
          component: Shipments,
          frn: orderShipmentFRN,
        }),
        route.read({
          name: 'order-shipments',
          path: 'shipments',
          component: Shipments,
          frn: orderShipmentFRN,
        }),

      ])
    ]);
}

const orderRoutes = (claims: Claims) => {


  return (
    <div>
      <FoxRoute name='carts-base' path='carts'>
        <FoxRoute name='carts-list-pages' component={CartsListPage}>
          <IndexRoute name='carts' component={Carts} />
        </FoxRoute>
        <FoxRoute name='cart' path=':cart' component={Cart}>
          <IndexRoute name='cart-details' component={CartDetails}/>
          <FoxRoute name='cart-notes' path='notes' component={Notes}/>
          <FoxRoute name='cart-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
        </FoxRoute>
      </FoxRoute>
      <Route name="orders-base" path="orders">
        <Route name="new-order" path="new" component={NewOrder}/>
        <Route name="orders-list-pages" component={OrdersListPage}>
          <IndexRoute name="orders" component={Orders}/>
          <Route name="orders-activity-trail" path="activity-trail" dimension="order"
                 component={ActivityTrailPage}/>
        </Route>

        <Route name="order" path=":order" component={Order}>
          <IndexRoute name="order-details" component={OrderDetails}/>
          <Route name="order-shipments" path="shipments" component={Shipments}/>
          <Route name="order-notes" path="notes" component={Notes}/>
          <Route name="order-activity-trail" path="activity-trail" component={ActivityTrailPage}/>
        </Route>
      </Route>
    </div>
  );
};

export default orderRoutes;
