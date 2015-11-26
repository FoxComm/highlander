import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import { Link, IndexLink } from '../link';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import RemorseTimer from './remorseTimer';
import { connect } from 'react-redux';
import { DateTime } from '../common/datetime';
import LocalNav from '../local-nav/local-nav';
import { PanelList, PanelListItem } from '../panel/panel-list';
import SectionTitle from '../section-title/section-title';
import * as orderActions from '../../modules/orders/details';
import * as shippingMethodActions from '../../modules/orders/shipping-methods';
import * as skusActions from '../../modules/skus';

const mapStateToProps = (state) => {
  return {
    order: state.orders.details,
    shippingMethods: state.orders.shippingMethods,
    skusActions: state.skusActions
  };
};

const mapDispatchToProps = {...orderActions, ...shippingMethodActions, ...skusActions};

@connect(mapStateToProps, mapDispatchToProps)
export default class Order extends React.Component {
  static propTypes = {
    params: PropTypes.shape({
      order: PropTypes.string.isRequired
    }).isRequired,
    order: PropTypes.shape({
      currentOrder: PropTypes.object
    }),
    children: PropTypes.node,
    updateOrder: PropTypes.func,
    fetchOrder: PropTypes.func
  };

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

  get remorseTimer() {
    if (this.order.id && this.order.orderStatus === 'remorseHold') {
      const refNum = this.order.referenceNumber;
      return <RemorseTimer initialEndDate={this.order.remorsePeriodEnd}
                           onIncreaseClick={ () => this.props.increaseRemorsePeriod(refNum) }/>;
    }
  }

  get viewers () {
    if (this.order.id) return <Viewers model='orders' modelId={this.order.id}/>;
  }

  get subNav() {
    if (this.order.id) {
      const content = React.cloneElement(this.props.children, {...this.props, entity: this.order});
      let params = {order: this.order.referenceNumber};

      return (
        <div>
          <LocalNav>
            <IndexLink to="order-details" params={params}>Details</IndexLink>
            <a href="">Shipments</a>
            <Link to="order-returns" params={params}>Returns</Link>
            <Link to="order-notifications" params={params}>Transaction Notifications</Link>
            <Link to="order-notes" params={params}>Notes</Link>
            <Link to="order-activity-trail" params={params}>Activity Trail</Link>
          </LocalNav>
          <div className="fc-grid">
            <div className="fc-col-md-1-1">
              {content}
            </div>
          </div>
        </div>
      );
    }
  }

  @autobind
  onStatusChange(value) {
    this.props.updateOrder(this.orderRefNum, {status: value});
  }

  componentDidMount() {
    this.props.fetchOrder(this.orderRefNum);
  }

  render() {
    const order = this.order;

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
      <Dropdown
        name="orderStatus"
        items={orderStatuses}
        placeholder={'Order status'}
        value={order.orderStatus}
        onChange={this.onStatusChange}
      />
    );

    return (
      <div className="fc-order">
        {this.viewers}
        <SectionTitle title={`Order ${this.orderRefNum}`}>
          {this.remorseTimer}
        </SectionTitle>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <PanelList>
              <PanelListItem title="Order Status">{orderStatus}</PanelListItem>
              <PanelListItem title="Shipment Status">{order.shippingStatus}</PanelListItem>
              <PanelListItem title="Payment Status">{order.paymentStatus}</PanelListItem>
              <PanelListItem title="Fraud Score">{order.fraudScore}</PanelListItem>
              <PanelListItem title="Date/Time Placed"><DateTime value={order.createdAt} /></PanelListItem>
            </PanelList>
          </div>
        </div>
        {this.subNav}
      </div>
    );
  }
}
