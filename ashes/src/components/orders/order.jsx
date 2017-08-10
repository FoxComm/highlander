// libs
import React, { Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { trackEvent } from 'lib/analytics';

import { allowedStateTransitions } from '../../paragons/order';

// components
import { Dropdown } from '../dropdown';
import RemorseTimer from './remorseTimer';
import { DateTime } from 'components/utils/datetime';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import StateComponent, { states } from '../common/state';
import ConfirmationModal from 'components/core/confirmation-modal';
import Spinner from 'components/core/spinner';
import Error from 'components/errors/error';

// helpers
import { getClaims, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

// redux
import * as orderActions from 'modules/orders/details';

import s from './order.css';

// types
import type { StateToProps, DispatchToProps, Props, StateType, ReduxState, OrderType } from './orderTypes';

const shippingClaims = readAction(frn.mdl.shipment);
const fraudClaims = readAction(frn.oms.fraud);

const orderRefNum = props => {
  return props.params.order;
};

const mapStateToProps = (state: ReduxState): StateToProps => {
  return {
    details: state.orders.details,
    isFetching: _.get(state.asyncActions, 'getOrder.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getOrder.err', null),
  };
};

const mapDispatchToProps: DispatchToProps = { ...orderActions };

@connect(mapStateToProps, mapDispatchToProps)
export default class Order extends React.Component {
  props: Props;
  state: StateType = {
    newOrderState: null,
  };

  updateInterval = null;

  componentDidMount() {
    this.props.clearFetchErrors();
    this.props.fetchOrder(this.orderRefNum);
  }

  componentWillReceiveProps(nextProps: Props): void {
    if (this.orderRefNum != orderRefNum(nextProps)) {
      this.props.fetchOrder(orderRefNum(nextProps));
    }
    if (_.get(nextProps, 'details.order.state') !== 'remorseHold') {
      if (this.updateInterval != null) {
        clearInterval(this.updateInterval);
        this.updateInterval = null;
      }
    }
  }

  componentWillUnmount(): void {
    if (this.updateInterval != null) {
      clearInterval(this.updateInterval);
      this.updateInterval = null;
    }
  }

  get orderRefNum(): string {
    return orderRefNum(this.props);
  }

  get order(): OrderType {
    return this.props.details.order;
  }

  get remorseTimer(): ?Element<*> {
    if (this.order.isRemorseHold) {
      const refNum = this.order.referenceNumber;
      const handleIncreaseClick = () => {
        trackEvent('Orders', 'click_remorse_timer_extend', 'Remorse Timer extend click');
        this.props.increaseRemorsePeriod(refNum);
      };
      return (
        <RemorseTimer
          initialEndDate={this.order.remorsePeriodEnd}
          onIncreaseClick={handleIncreaseClick}
          onCountdownFinished={this.onRemorseCountdownFinish}
        />
      );
    }
  }

  get renderDetails(): Element<*> {
    const details = React.cloneElement(this.props.children, { ...this.props, entity: this.order });

    return (
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          {details}
        </div>
      </div>
    );
  }

  get subNav(): Element<*> {
    return <SubNav order={this.order} />;
  }

  @autobind
  onRemorseCountdownFinish() {
    if (this.updateInterval == null) {
      this.updateInterval = setInterval(() => this.props.fetchOrder(this.orderRefNum), 5000);
    }
  }

  @autobind
  onStateChange(value: string): void {
    trackEvent('Orders', 'change_orders_state_by_dropdown');
    this.setState({
      newOrderState: value,
    });
  }

  @autobind
  confirmStateChange(): void {
    this.setState({
      newOrderState: null
    });

    if (this.state.newOrderState == 'shipped') {
      this.props.updateShipments(this.orderRefNum);
    }

    this.props.updateOrder(this.orderRefNum, { state: this.state.newOrderState });
  }

  @autobind
  cancelStateChange(): void {
    this.setState({
      newOrderState: null
    });
  }

  get orderStateDropdown(): Element<StateComponent|Dropdown> {
    const order = this.order;
    const claims = getClaims();

    if (order.orderState === 'canceled' ||
        order.orderState === 'shipped') {
      return <StateComponent stateId="fct-order-state__value" value={order.shippingState} model="order" />;
    }

    let holdStates = ['manualHold'];
    if (isPermitted(fraudClaims, claims)) {
      holdStates = [...holdStates, 'fraudHold'];
    }

    const visibleAndSortedOrderStates = [
      ...holdStates,
      'fulfillmentStarted',
      'shipped',
      'canceled',
    ].filter(state => {
      return order.orderState in allowedStateTransitions &&
        allowedStateTransitions[order.orderState].indexOf(state) != -1;
    });

    return (
      <Dropdown
        id="fct-order-state-dd"
        dropdownValueId="fct-order-state__value"
        name="orderState"
        items={_.map(visibleAndSortedOrderStates, state => [state, states.order[state]])}
        placeholder={'Order state'}
        value={order.orderState}
        onChange={this.onStateChange}
        changeable={false}
        renderNullTitle={(value, placeholder) => {
          if (value in states.order) {
            return states.order[value];
          }
          return placeholder;
        }}
      />
    );
  }

  get statusHeader(): ?Element<*> {
    const order = this.order;
    const claims = getClaims();
    const shippingState = isPermitted(shippingClaims, claims)
      ? (
          <PanelListItem title="Shipping State">
            <StateComponent stateId="order-shipping-state-value" value={order.shippingState} model="shipment" />
          </PanelListItem>
        ) : null;

    return (
      <div className="fc-grid fc-grid-gutter">
        <div className="fc-col-md-1-1">
          <PanelList>
            <PanelListItem title="Order State">
              {this.orderStateDropdown}
            </PanelListItem>
            {shippingState}
            <PanelListItem title="Payment State">
              <StateComponent stateId="order-payment-state-value" value={order.paymentState} model="payment" />
            </PanelListItem>
            <PanelListItem title="Date/Time Placed">
              <DateTime value={order.placedAt} />
            </PanelListItem>
          </PanelList>
        </div>
      </div>
    );
  }

  get contents(): Element<*> {
    const order = this.order;
    return (
      <div>
        <PageTitle title={`${order.title} ${this.orderRefNum}`}>
          {this.remorseTimer}
        </PageTitle>
        {this.statusHeader}
        <div>
          {this.subNav}
          {this.renderDetails}
        </div>
        <ConfirmationModal
          isVisible={this.state.newOrderState != null}
          title="Change Order State ?"
          label={`Are you sure you want to change order state to ${states.order[this.state.newOrderState]} ?`}
          confirmLabel="Yes, Change"
          onCancel={this.cancelStateChange}
          onConfirm={this.confirmStateChange}
        />
      </div>
    );
  }

  get body(): Element<any> {
    if (this.props.isFetching || !this.order) {
      return <Spinner className={s.spinner} />;
    }
    if (this.props.fetchError) {
      return <Error notFound={`There is no order with reference number ${this.orderRefNum}`} />;
    }
    return this.contents;
  }

  render(): Element<*> {
    return (
      <div className="fc-order">
        {this.body}
      </div>
    );
  }
}
