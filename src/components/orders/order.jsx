'use strict';

import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as actionCreators from '../../actions/orders';
import { Link, IndexLink } from '../link';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import RemorseTimer from './remorseTimer';

function mapStateToProps(state) {
  return {
    order: state.order || {}
  };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(actionCreators, dispatch) };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class Order extends React.Component {
  static propTypes = {
    params: PropTypes.shape({
      order: PropTypes.string.isRequired
    }).isRequired,
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
    return this.props.order.item;
  }

  componentDidMount() {
    this.props.actions.fetchOrderIfNeeded(this.orderRefNum);
  }

  // onConfirmChange(success) {
  //   if (!success) return;

  //   this.patchOrder();
  // }

  // patchOrder() {
  //   OrderActions.updateOrderStatus(this.orderRefNum, this.state.pendingStatus);
  //   this.setState({
  //     pendingStatus: null
  //   });
  // }

  // prepareStatusChange(status) {
  //   this.setState({
  //     pendingStatus: status
  //   });
  //   let options = status !== 'canceled' ? this.changeOptions : this.cancelOptions;

  //   dispatch('toggleModal', <ConfirmModal callback={this.onConfirmChange.bind(this)} details={options} />);
  // }

  // changeOrderStatus(event) {
  //   let status = event.target.value;
  //   this.prepareStatusChange(status);
  // }

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

    const content = React.cloneElement(this.props.children, {order, modelName: 'order' });

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

    // TODO: Re-enable
    // if (OrderStore.editableStatusList.indexOf(order.orderStatus) !== -1) {
    //   orderStatus = (
    //     <select name="orderStatus" value={order.orderStatus} onChange={this.changeOrderStatus.bind(this)}>
    //       {OrderStore.selectableStatusList.map((status, idx) => {
    //         if (
    //           (order.orderStatus === 'fulfillmentStarted') &&
    //           (['fulfillmentStarted', 'canceled'].indexOf(status) === -1)
    //         ) {
    //           return '';
    //         } else {
    //           return <option key={`${idx}-${status}`} value={status}>{OrderStore.statuses[status]}</option>;
    //         }
    //       })}
    //     </select>
    //   );
    // } else {
    //   orderStatus = OrderStore.statuses[order.orderStatus];
    // }

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
        </div>
        {subNav}
      </div>
    );
  }
}
