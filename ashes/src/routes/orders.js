/* @flow */

import React, { Component, Element } from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

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

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const cartRoutes =
    router.read('carts-base', { path: 'carts', frn: frn.oms.cart }, [
      router.read('carts-list-pages', { component: CartsListPage }, [
        router.read('carts', { component: Carts, isIndex: true }),
        router.read('carts-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.cart,
        }),
      ]),
      router.read('cart', { path: ':cart', component: Cart}, [
        router.read('cart-details', { component: CartDetails, isIndex: true}),
        router.read('cart-notes', { path: 'notes', component: Notes}),
        router.read('cart-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.cart,
        }),
      ]),
    ]);

  const orderRoutes =
    router.read('orders-base', { path: 'orders', frn: frn.oms.order }, [
      router.create('new-order', { path: 'new', component: NewOrder }),
      router.read('orders-list-pages', { component: OrdersListPage }, [
        router.read('orders', { component: Orders, isIndex: true }),
        router.read('orders-activity-trail', {
          path: 'activity-trail',
          dimension: 'order',
          component: ActivityTrailPage,
          frn: frn.activity.order,
        }),
      ]),
      router.read('order', { path: ':order', component: Order }, [
        router.read('order-details', { component: OrderDetails, isIndex: true }),
        router.read('order-shipments', { path: 'shipments', component: Shipments, frn: frn.mdl.shipment }),
        router.read('order-notes', { path: 'notes', component: Notes, frn: frn.note.order }),
        router.read('order-activity-trail', {
          path: 'activity-trail',
          components: ActivityTrailPage,
          frn: frn.activity.order,
        }),
      ]),
    ]);

  return (
    <div>
      {cartRoutes}
      {orderRoutes}
    </div>
  );
};

const orderRoutes = (claims: Claims) => {
  return getRoutes(claims);
};

export default orderRoutes;
