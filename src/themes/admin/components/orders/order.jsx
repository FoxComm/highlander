'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import OrderStore from './store';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import Countdown from '../countdown/countdown';

const confirmEvent = 'confirm-change';
const changeOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to change the order status?',
  cancel: 'Cancel',
  proceed: 'Yes'
};
const cancelOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to cancel the order?',
  cancel: 'No, Don\'t Cancel',
  proceed: 'Yes, Cancel Order'
};

export default class Order extends React.Component {

  constructor(props) {
    super(props);
    this.onChangeOrderStore = this.onChangeOrderStore.bind(this);
    this.onConfirmChange = this.onConfirmChange.bind(this);
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
    OrderStore.listenToEvent('change', this);
    listenTo(confirmEvent, this);
    OrderStore.fetch(orderId);
  }

  componentWillUnmount() {
    OrderStore.stopListeningToEvent('change', this);
    stopListeningTo(confirmEvent, this);
  }

  onChangeOrderStore(order) {
    this.setState({
      order: order,
      customer: order.customer
    });
  }

  onConfirmChange() {
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

  prepareStatusChange(status) {
    this.setState({
      pendingStatus: status
    });
    let options = status !== 'canceled' ? changeOptions : cancelOptions;

    dispatch('toggleModal', <ConfirmModal event={confirmEvent} details={options} />);
  }

  changeOrderStatus(event) {
    let status = event.target.value;
    this.prepareStatusChange(status);
  }

  render() {
    let
      order         = this.state.order,
      subNav        = null,
      viewers       = null,
      orderStatus   = null,
      countdown     = null;

    if (order.id) {
      let params = {order: order.id};

      subNav = (
        <div className="gutter">
          <ul className="tabbed-nav">
            <li><Link to="order-details" params={params}>Details</Link></li>
            <li><a href="">Shipments</a></li>
            <li><a href="">Returns</a></li>
            <li><Link to="order-notifications" params={params}>Transcation Notifications</Link></li>
            <li><Link to="order-notes" params={params}>Notes</Link></li>
            <li><Link to="order-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          <RouteHandler order={order} modelName="order"/>
        </div>
      );

      viewers = <Viewers model='orders' modelId={order.id}/>;

      if (order.orderStatus === 'remorseHold') countdown = <Countdown endDate={order.remorseEnd} />;
    }

    if (OrderStore.editableStatusList.indexOf(order.orderStatus) !== -1) {
      orderStatus = (
        <select name="orderStatus" value={order.orderStatus} onChange={this.changeOrderStatus.bind(this)}>
          {OrderStore.selectableStatusList.map((status, idx) => {
            if (order.orderStatus === 'fulfillmentStarted' && ['fulfillmentStarted', 'canceled'].indexOf(status) === -1) return '';
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
        <div className="gutter title">
          <div>
            <h1>Order {order.referenceNumber}</h1>
            {countdown}
          </div>
          <button className='btn cancel' onClick={() => { this.prepareStatusChange('canceled'); }}>Cancel Order</button>
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
