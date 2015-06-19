'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';
import OrderStore from './store';
import OrderViewers from './viewers';

const changeEvent = 'change-order-store';

export default class Order extends React.Component {

  constructor(props) {
    super(props);
    this.onChangeOrderStore = this.onChangeOrderStore.bind(this);
    this.state = {
      order: null
    };
  }

  componentDidMount() {
    let
      { router }  = this.context,
      orderId     = router.getCurrentParams().order;
    listenTo(changeEvent, this);
    OrderStore.fetch(orderId);
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
  }

  onChangeOrderStore(order) {
    this.setState({order: order});
  }

  render() {
    if (!this.state.order) return <div id="order"></div>;
    let
      order     = this.state.order,
      customer  = this.state.order.customer;
    return (
      <div id="order">
        <OrderViewers orderId={order.id}/>
        <div className="gutter">
          <h1>Order {order.orderId}<em>for{`${customer.firstName} ${customer.lastName}`}</em></h1>
          <time dateTime={order.date}>{order.date}</time>
        </div>
        <div className="gutter sub-nav">
          <ul>
            <li><a href="">Details</a></li>
            <li><a href="">Shipments</a></li>
            <li><a href="">Returns</a></li>
            <li><a href="">Emails</a></li>
            <li><Link to="notes" params={{order: order.id}}>Notes</Link></li>
            <li><a href="">Activity Trail</a></li>
          </ul>
          <RouteHandler/>
        </div>
      </div>
    );
  }
}

Order.contextTypes = {
  router: React.PropTypes.func
};
