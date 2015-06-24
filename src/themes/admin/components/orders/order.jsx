'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';
import OrderStore from './store';
import Viewers from '../viewers/viewers';
import Countdown from '../countdown/countdown';

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
      order         = this.state.order,
      subNav        = null,
      viewers       = null,
      countdown     = null;

    if (order.id) {
      subNav = (
        <div className="gutter">
          <ul className="tabbed-nav">
            <li><Link to="order-details" params={{order: order.orderId}}>Details</Link></li>
            <li><a href="">Shipments</a></li>
            <li><a href="">Returns</a></li>
            <li><Link to="order-notifications" params={{order: order.id}}>Transcation Notifications</Link></li>
            <li><Link to="order-notes" params={{order: order.orderId}}>Notes</Link></li>
            <li><a href="">Activity Trail</a></li>
          </ul>
          <RouteHandler/>
        </div>
      );

      viewers = <Viewers model='orders' modelId={order.id}/>;

      if (order.orderStatus === 'remorseHold') countdown = <Countdown endDate={order.remorseEnd} />;
    }

    return (
      <div id="order">
        {viewers}
        <div className="gutter">
          <h1>Order {order.orderId}</h1>
          {countdown}
        </div>
        <div className="gutter statuses">
          <dl>
            <dt>Order Status</dt>
            <dd>
              <select name="orderStatus">
                <option value="remorseHold">Remorse Hold</option>
                <option value="cancelled">Cancelled</option>
              </select>
            </dd>
          </dl>
          <dl>
            <dt>Shipment Status</dt>
            <dd>{order.shippingStatus}</dd>
          </dl>
          <dl>
            <dt>Payment Status</dt>
            <dd>{order.paymentStatus}</dd>
          </dl>
          <dl>
            <dt>Fraud Score</dt>
            <dd>{order.fraudScore}</dd>
          </dl>
        </div>
        {subNav}
      </div>
    );
  }
}

Order.contextTypes = {
  router: React.PropTypes.func
};
