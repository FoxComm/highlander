// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { trackEvent } from 'lib/analytics';

import { allowedStateTransitions } from '../../paragons/order';

// components
import { Dropdown } from '../dropdown';
import RemorseTimer from './remorseTimer';
import { DateTime } from '../common/datetime';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import State, { states } from '../common/state';
import ConfirmationDialog from '../modal/confirmation-dialog';
import WaitAnimation from '../common/wait-animation';
import Error from 'components/errors/error';

// helpers
import { getClaims, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

// redux
import * as orderActions from 'modules/orders/details';

const shippingClaims = readAction(frn.mdl.shipment);
const fraudClaims = readAction(frn.oms.fraud);

const orderRefNum = props => {
  return props.params.order;
};

const mapStateToProps = (state) => {
  return {
    details: state.orders.details,
    isFetching: _.get(state.asyncActions, 'getOrder.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getOrder.err', null),
  };
};

const mapDispatchToProps = { ...orderActions };

@connect(mapStateToProps, mapDispatchToProps)
export default class Order extends React.Component {
  static propTypes = {
    params: PropTypes.shape({
      order: PropTypes.string.isRequired
    }).isRequired,
    details: PropTypes.shape({
      order: PropTypes.object,
    }),
    children: PropTypes.node,
    updateOrder: PropTypes.func,
    fetchOrder: PropTypes.func,
    clearFetchErrors: PropTypes.func,
    increaseRemorsePeriod: PropTypes.func
  };

  constructor(...args) {
    super(...args);
    this.state = {
      newOrderState: null,
    };
  }

  updateInterval = null;

  componentDidMount() {
    this.props.clearFetchErrors();
    this.props.fetchOrder(this.orderRefNum);
  }

  componentWillReceiveProps(nextProps) {
    if (this.orderRefNum != orderRefNum(nextProps)) {
      this.props.fetchOrder(orderRefNum(nextProps));
    }
    if (_.get(nextProps, 'details.order.state') !== 'remorseHold') {
      clearInterval(this.updateInterval);
      this.updateInterval = null;
    }
  }

  componentWillUnmount() {
    if (this.state.updateInterval != null) {
      clearInterval(this.updateInterval);
      this.updateInterval = null;
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
    return this.props.details.order;
  }

  get remorseTimer() {
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
          onCountdownFinished={() => this.onRemorseCountdownFinish()}
        />
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
  onRemorseCountdownFinish() {
    if (this.updateInterval == null) {
      this.updateInterval = setInterval(() => this.props.fetchOrder(this.orderRefNum), 5000);
    }
  }

  @autobind
  onStateChange(value) {
    trackEvent('Orders', 'change_orders_state_by_dropdown');
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
    const claims = getClaims();

    if (order.orderState === 'canceled' ||
        order.orderState === 'shipped') {
      return <State value={order.shippingState} model="order" />;
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

  get statusHeader() {
    const order = this.order;
    const claims = getClaims();
    const shippingState = isPermitted(shippingClaims, claims)
      ? (
          <PanelListItem title="Shipping State">
            <State value={order.shippingState} model="shipment" />
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
              <State value={order.paymentState} model="payment" />
            </PanelListItem>
            <PanelListItem title="Date/Time Placed">
              <DateTime value={order.placedAt} />
            </PanelListItem>
          </PanelList>
        </div>
      </div>
    );
  }

  get contents() {
    const order = this.order;
    return (
      <div>
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
          onCancel={this.cancelStateChange}
          confirmAction={this.confirmStateChange}
        />
      </div>
    );
  }

  get body() {
    if (this.props.isFetching !== false) {
      return <WaitAnimation />;
    }
    if (this.props.fetchError) {
      return <Error notFound={`There is no order with reference number ${this.orderRefNum}`} />;
    }
    return this.contents;
  }

  render() {
    return (
      <div className="fc-order">
        {this.body}
      </div>
    );
  }
}
