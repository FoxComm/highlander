'use strict';

import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import RemorseTimer from './remorseTimer';
import { connect } from 'react-redux';
import * as orderActions from '../../modules/orders/order';
import DateTime from '../datetime/datetime';

@connect(state => ({order: state.orders.order}), orderActions)
export default class Order extends React.Component {
  static propTypes = {
    params: PropTypes.shape({
      order: PropTypes.string.isRequired
    }).isRequired,
    order: PropTypes.shape({
      currentOrder: PropTypes.object
    }),
    fetchOrderIfNeeded: PropTypes.func,
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
  }

  get changeOptions() {
    return {
      header: 'Confirm',
      body: 'Are you sure you want to change the order status?',
      cancel: 'Cancel',
      proceed: 'Yes'
    };
  }

  get cancelOptions() {
    return {
      header: 'Confirm',
      body: 'Are you sure you want to cancel the order?',
      cancel: 'No, Don\'t Cancel',
      proceed: 'Yes, Cancel Order'
    };
  }

  get orderRefNum() {
    return this.props.params.order;
  }

  get order() {
    return this.props.order.currentOrder;
  }

  componentDidMount() {
    this.props.fetchOrderIfNeeded(this.orderRefNum);
  }

  render() {
    let
      order         = this.order,
      subNav        = null,
      viewers       = null,
      orderStatus   = null,
      remorseTimer  = null;

    if (!order) {
      return <div className="fc-order"></div>;
    }

    const content = React.cloneElement(this.props.children, {order, modelName: 'order', ...this.props});

    if (order.id) {
      let params = {order: order.referenceNumber};

      subNav = (
        <div className="gutter">
          <ul className="fc-tabbed-nav">
            <li><IndexLink to="order-details" params={params}>Details</IndexLink></li>
            <li><a href="">Shipments</a></li>
            <li><Link to="order-returns" params={params}>Returns</Link></li>
            <li><Link to="order-notifications" params={params}>Transaction Notifications</Link></li>
            <li><Link to="order-notes" params={params}>Notes</Link></li>
            <li><Link to="order-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          {content}
        </div>
      );

      viewers = <Viewers model='orders' modelId={order.id}/>;

      if (order.orderStatus === 'remorseHold') remorseTimer = <RemorseTimer endDate={order.remorseEnd} />;
    }

    return (
      <div className="fc-order">
        {viewers}
          <div className="gutter title">
          <div>
            <h1>Order {order.referenceNumber}</h1>
            {remorseTimer}
          </div>
        </div>
        <div className="gutter statuses">
          <dl>
            <dt>Order Status</dt>
            <dd>{orderStatus}</dd>
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
          <dl>
            <dt>Date/Time Placed</dt>
            <dd><DateTime value={order.createdAt} /></dd>
          </dl>
        </div>
        {subNav}
      </div>
    );
  }
}
