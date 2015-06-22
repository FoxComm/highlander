'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';
import OrderStore from './store';
import Viewers from '../viewers/viewers';

const changeEvent = 'change-order-store';

export default class Order extends React.Component {

  constructor(props) {
    super(props);
    this.onChangeOrderStore = this.onChangeOrderStore.bind(this);
    this.state = {
      order: {},
      customer: {}
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
    this.setState({
      order: order,
      customer: order.customer
    });
  }

  render() {
    let
      order     = this.state.order,
      customer  = this.state.customer,
      subNav    = null,
      viewers   = null;

    if (order.id) {
      subNav = (
        <div className="gutter sub-nav">
          <ul>
            <li><a href="">Details</a></li>
            <li><a href="">Shipments</a></li>
            <li><a href="">Returns</a></li>
            <li><Link to="order-notifications" params={{order: order.id}}>Transcation Notifications</Link></li>
            <li><Link to="notes" params={{order: order.id}}>Notes</Link></li>
            <li><a href="">Activity Trail</a></li>
          </ul>
          <RouteHandler/>
        </div>
      );

      viewers = <Viewers model='orders' modelId={order.id}/>;
    }

    return (
      <div id="order">
        {viewers}
        <div className="gutter">
          <h1>Order {order.orderId}<em>for{`${customer.firstName} ${customer.lastName}`}</em></h1>
          <time dateTime={order.date}>{order.date}</time>
        </div>
        {subNav}
      </div>
    );
  }
}

Order.contextTypes = {
  router: React.PropTypes.func
};
