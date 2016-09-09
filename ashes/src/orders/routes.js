/* @flow */

import React, { Component, Element } from 'react';
import { Route, IndexRoute } from 'react-router';

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

const orderRoutes = () => {
  return (
    <div>
      <Route name='carts-base' path='carts'>
        <Route name='carts-list-pages' component={CartsListPage}>
          <IndexRoute name='carts' component={Carts} />
        </Route>
        <Route name='cart' path=':cart' component={Cart}>
          <IndexRoute name='cart-details' component={CartDetails}/>
          <Route name='cart-notes' path='notes' component={Notes}/>
          <Route name='cart-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
        </Route>
      </Route>
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
