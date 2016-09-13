/* @flow */

import React, { Component, Element } from 'react';
import { Route, IndexRoute } from 'react-router';
import _ from 'lodash';

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

  read(name: string, params: Object, children) {
    const { isIndex, frn, ...rest } = params;
    const RouteComponent = isIndex ? IndexRoute : Route;
    console.log(`Creating read route: ${name}`);
    return (
      <RouteComponent name={name} {...rest}>
        {children}
      </RouteComponent>
    );
  }

  create(name: string, params: Object, children) {
    const { isIndex, frn, ...rest } = params;
    const RouteComponent = isIndex ? IndexRoute : Route;
    console.log(`Creating create route: ${name}`);
    return (
      <RouteComponent name={name} {...rest}>
        {children}
      </RouteComponent>
    );
  }
}

const getRoutes = (jwt: Object) => {
  let route = new Routerr(jwt);

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
    route.read('orders-base', { path: 'orders', frn: orderFRN }, [
      route.create('new-order', { path: 'new', component: NewOrder }),
      route.read('orders-list-pages', { component: OrdersListPage }, [
        route.read('orders', { component: Orders, isIndex: true }),
        route.read('orders-activity-trail', { path: 'activity-trail', component: ActivityTrailPage, frn: activityFRN }),
      ]),
      route.read('order', { path: ':order', component: Order }, [
        route.read('order-details', { component: OrderDetails, isIndex: true }),
        route.read('order-shipments', { path: 'shipments', component: Shipments, frn: shipmentFRN }),
        route.read('order-notes', { path: 'notes', component: Notes, frn: noteFRN }),
        route.read('order-activity-trail', { path: 'activity-trail', components: ActivityTrailPage, frn: activityFRN }),
      ]),
    ]);

  return (
    <div>
      {cartRoutes}
      {orderRoutes}
    </div>
  );
}

const orderRoutes = (claims: Claims) => {
  return getRoutes(claims);
};

export default orderRoutes;
