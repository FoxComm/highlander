import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import RemorseTimer from './remorseTimer';
import { connect } from 'react-redux';
import { DateTime } from '../common/datetime';
import { PanelList, PanelListItem } from '../panel/panel-list';
import SectionTitle from '../section-title/section-title';
import * as orderActions from '../../modules/orders/details';
import * as shippingMethodActions from '../../modules/orders/shipping-methods';
import * as skusActions from '../../modules/skus';
import SubNav from './sub-nav';
import classNames from 'classnames';
import Status, { statuses } from '../common/status';
import * as paymentMethodActions from '../../modules/orders/payment-methods';

const mapStateToProps = (state) => {
  return {
    order: state.orders.details,
    shippingMethods: state.orders.shippingMethods,
    skusActions: state.skusActions,
    payments: state.orders.paymentMethods
  };
};

const mapDispatchToProps = {...orderActions, ...shippingMethodActions, ...skusActions, ...paymentMethodActions};

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
    fetchOrder: PropTypes.func,
    increaseRemorsePeriod: PropTypes.func
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
    if (this.order.isRemorseHold) {
      const refNum = this.order.referenceNumber;
      return (
        <RemorseTimer initialEndDate={this.order.remorsePeriodEnd}
                      onIncreaseClick={ () => this.props.increaseRemorsePeriod(refNum) }/>
      );
    }
  }

  get details() {
    const details = React.cloneElement(this.props.children, {...this.props, entity: this.order});
    return (
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          {details}
        </div>
      </div>
    );
  }

  get subNav() {
    return <SubNav order={this.order} />;
  }

  get statusHeader() {
    const order = this.order;

    if (order.isCart) {
      return;
    }

    const orderStatuses = statuses.order;

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
      <div className="fc-grid fc-grid-gutter">
        <div className="fc-col-md-1-1">
          <PanelList>
            <PanelListItem title="Order State">
              {orderStatus}
            </PanelListItem>
            <PanelListItem title="Shipment State">
              <Status value={order.shippingStatus} model={"shipment"} />
            </PanelListItem>
            <PanelListItem title="Payment State">
              <Status value={order.paymentStatus} model={"payment"} />
            </PanelListItem>
            <PanelListItem title="Fraud Score">
              {order.fraudScore}
            </PanelListItem>
            <PanelListItem title="Date/Time Placed">
              <DateTime value={order.placedAt} />
            </PanelListItem>
          </PanelList>
        </div>
      </div>
    );
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
    const className = classNames('fc-order', {'fc-cart': order.isCart});

    if (_.isEmpty(order)) {
      return <div className={className}></div>;
    }

    return (
      <div className={className}>
        <SectionTitle title={`${order.title} ${this.orderRefNum}`}>
          {this.remorseTimer}
        </SectionTitle>
        {this.statusHeader}
        <div>
          {this.subNav}
          {this.details}
        </div>
      </div>
    );
  }
}
