'use strict';

import React, { PropTypes } from 'react';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import { Link, IndexLink } from '../link';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import RemorseTimer from './remorseTimer';
import { connect } from 'react-redux';
import * as orderActions from '../../modules/order';
import { DateTime } from '../common/datetime';

@connect(state => ({order: state.order}), orderActions)
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
      remorseTimer  = null;

    if (!order) {
      return <div className="fc-order"></div>;
    }

    // order status render
    const orderStatuses = {
      cart: 'Cart',
      remorseHold: 'Remorse Hold',
      manualHold: 'Manual Hold',
      fraudHold: 'Fraud Hold',
      fulfillmentStarted: 'Fulfillment Started',
      canceled: 'Canceled',
      partiallyShipped: 'Partially Shipped',
      shipped: 'Shipped'
    };

    const orderStatus = (
      <Dropdown name="orderStatus" items={orderStatuses} placeholder={'Order status'} value={order.orderStatus}/>
    );

    const content = React.cloneElement(this.props.children, {...this.props, entity: order});

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
            {remorseTimer}
            <h1>Order {this.orderRefNum}</h1>
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
