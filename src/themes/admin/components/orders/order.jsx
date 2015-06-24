'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import OrderStore from './store';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';

const changeEvent = 'change-order-store';
const confirmEvent = 'confirm-cancel';
const modalOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to cancel the order?',
  cancel: 'No, Don\'t Cancel',
  proceed: 'Yes, Cancel Order'
};

export default class Order extends React.Component {

  constructor(props) {
    super(props);
    this.onChangeOrderStore = this.onChangeOrderStore.bind(this);
    this.onConfirmCancel = this.onConfirmCancel.bind(this);
    this.state = {
      order: {},
      customer: {},
      pendingStatus: null
    };
  }

  componentDidMount() {
    let
      { router }  = this.context,
      orderId     = router.getCurrentParams().order;
    listenTo(changeEvent, this);
    listenTo(confirmEvent, this);
    OrderStore.fetch(orderId);
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
    stopListeningTo(confirmEvent, this);
  }

  onChangeOrderStore(order) {
    this.setState({
      order: order,
      customer: order.customer
    });
  }

  onConfirmCancel() {
    dispatch('toggleModal', null);
    this.patchOrder();
  }

  patchOrder() {
    OrderStore.patch(this.state.order.id, {
      'orderStatus': this.state.pendingStatus
    });
    this.setState({
      pendingStatus: null
    });
  }

  changeOrderStatus(event) {
    let status = event.target.value;
    this.setState({
      pendingStatus: status
    });
    if (status !== 'canceled') {
      this.patchOrder();
    } else {
      dispatch('toggleModal', <ConfirmModal event={confirmEvent} details={modalOptions} />);
    }
  }

  render() {
    let
      order         = this.state.order,
      subNav        = null,
      viewers       = null,
      orderStatus   = null;

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
    }

    if (OrderStore.editableStatusList.indexOf(order.orderStatus) !== -1) {
      orderStatus = (
        <select name="orderStatus" value={order.orderStatus} onChange={this.changeOrderStatus.bind(this)}>
          {OrderStore.selectableStatusList.map((status, idx) => {
            return <option key={`${idx}-${status}`} value={status}>{OrderStore.statuses[status]}</option>;
          })}
        </select>
      );
    } else {
      orderStatus = OrderStore.statuses[order.orderStatus];
    }

    return (
      <div id="order">
        {viewers}
        <div className="gutter">
          <h1>Order {order.orderId}</h1>
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
        </div>
        {subNav}
      </div>
    );
  }
}

Order.contextTypes = {
  router: React.PropTypes.func
};
