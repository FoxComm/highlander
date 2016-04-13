// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import classNames from 'classnames';

import { allowedStateTransitions } from '../../paragons/order';

// components
import { Dropdown, DropdownItem } from '../dropdown';
import Viewers from '../viewers/viewers';
import ConfirmModal from '../modal/confirm';
import RemorseTimer from './remorseTimer';
import { DateTime } from '../common/datetime';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import State, { states } from '../common/state';
import ConfirmationDialog from '../modal/confirmation-dialog';
import WaitAnimation from '../common/wait-animation';

// redux
import * as orderActions from '../../modules/orders/details';

const orderRefNum = props => {
  return props.params.order;
};

const mapStateToProps = (state) => {
  return {
    order: state.orders.details,
  };
};

const mapDispatchToProps = { ...orderActions };

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

  constructor(...args) {
    super(...args);
    this.state = {
      newOrderState: null,
    };
  }

  componentDidMount() {
    this.props.fetchOrder(this.orderRefNum);
  }

  componentWillReceiveProps(nextProps) {
    if (this.orderRefNum != orderRefNum(nextProps)) {
      this.props.fetchOrder(orderRefNum(nextProps));
    }
  }

  get changeOptions() {
    return {
      header: 'Confirm',
      body: 'Are you sure you want to change the order state?',
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
    return orderRefNum(this.props);
  }

  get order() {
    return this.props.order.currentOrder;
  }

  get remorseTimer() {
    if (this.order.isRemorseHold) {
      const refNum = this.order.referenceNumber;
      return (
        <RemorseTimer initialEndDate={this.order.remorsePeriodEnd}
                      onIncreaseClick={ () => this.props.increaseRemorsePeriod(refNum) } />
      );
    }
  }

  get details() {
    const details = React.cloneElement(this.props.children, { ...this.props, entity: this.order });
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

  @autobind
  onStateChange(value) {
    this.setState({
      newOrderState: value,
    });
  }

  @autobind
  confirmStateChange() {
    this.setState({
      newOrderState: null
    });
    this.props.updateOrder(this.orderRefNum, { state: this.state.newOrderState });
  }

  @autobind
  cancelStateChange() {
    this.setState({
      newOrderState: null
    });
  }

  get orderStateDropdown() {
    const order = this.order;

    if (order.orderState === 'canceled' ||
        order.orderState === 'fulfillmentStarted' ||
        order.orderState === 'shipped') {
      return <State value={order.shippingState} model={"order"} />;
    }

    const visibleAndSortedOrderStates = [
      'remorseHold',
      'manualHold',
      'fulfillmentStarted',
      'canceled',
    ].filter(state => {
      return order.orderState in allowedStateTransitions &&
        allowedStateTransitions[order.orderState].indexOf(state) != -1;
    });

    return (
      <Dropdown
        name="orderState"
        items={_.map(visibleAndSortedOrderStates, state => [state, states.order[state]])}
        placeholder={'Order state'}
        value={order.orderState}
        onChange={this.onStateChange}
        renderNullTitle={(value, placeholder) => {
          if (value in states.order) {
            return states.order[value];
          }
          return placeholder;
        }}
      />
    );
  }

  get statusHeader() {
    const order = this.order;

    if (order.isCart) {
      return;
    }

    return (
      <div className="fc-grid fc-grid-gutter">
        <div className="fc-col-md-1-1">
          <PanelList>
            <PanelListItem title="Order State">
              {this.orderStateDropdown}
            </PanelListItem>
            <PanelListItem title="Shipment State">
              <State value={order.shippingState} model={"shipment"} />
            </PanelListItem>
            <PanelListItem title="Payment State">
              <State value={order.paymentState} model={"payment"} />
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

  render() {
    const order = this.order;
    const className = classNames('fc-order', { 'fc-cart': order.isCart });

    if (this.props.order.isFetching) {
      return <div className={className}><WaitAnimation /></div>;
    }
    else if (_.isEmpty(order)) {
      return <div className={className}></div>;
    }

    return (
      <div className={className}>
        <PageTitle title={`${order.title} ${this.orderRefNum}`}>
          {this.remorseTimer}
        </PageTitle>
        {this.statusHeader}
        <div>
          {this.subNav}
          {this.details}
        </div>
        <ConfirmationDialog
          isVisible={this.state.newOrderState != null}
          header="Change Order State ?"
          body={`Are you sure you want to change order state to ${states.order[this.state.newOrderState]} ?`}
          cancel="Cancel"
          confirm="Yes, Change"
          cancelAction={this.cancelStateChange}
          confirmAction={this.confirmStateChange}
        />
      </div>
    );
  }
}
