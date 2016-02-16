import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import formatCurrency from '../../../lib/format-currency';
import _ from 'lodash';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';
import * as StoreCreditTotalsActions from '../../../modules/customers/store-credit-totals';

import DebitCredit from './debit-credit';

function mapActionsToCustomer(dispatch, actions, customerId) {
  return _.transform(actions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(customerId, ...args));
    };
  });
}

function mapStateToProps(state, props) {
  return {
    totals: state.customers.storeCreditTotals[props.customerId],
  };
}

function mapDispatchToProps(dispatch, props) {
  return {
    actions: {
      ...bindActionCreators(PaymentMethodActions, dispatch),
      ...mapActionsToCustomer(dispatch, StoreCreditTotalsActions, props.customerId),
    },
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class NewStoreCredit extends Component {
  static propTypes = {
    customerId: PropTypes.number.isRequired,
    order: PropTypes.shape({
      referenceNumber: PropTypes.string.isRequired,
    }).isRequired,
    totals: PropTypes.object.isRequired,

    actions: PropTypes.shape({
      addOrderStoreCreditPayment: PropTypes.func.isRequired,
      fetchTotals: PropTypes.func.isRequired,
    }).isRequired,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      amountToUse: 0,
    };
  }

  componentDidMount() {
    this.props.actions.fetchTotals();
  }

  get availableBalance() {
    return _.get(this.props, 'totals.totals.availableBalance', 0);
  }

  @autobind
  handleChange(amount) {
    this.setState({
      amountToUse: Math.min(amount, this.availableBalance),
    });
  }

  @autobind
  handleSubmit(event) {
    event.preventDefault();
    this.props.actions.addOrderStoreCreditPayment(
      this.props.order.referenceNumber,
      this.state.amountToUse);
  }

  render() {
    return (
      <div className="fc-order-apply-store-credit">
        <DebitCredit amountToUse={this.state.amountToUse}
                     availableBalance={this.availableBalance}
                     onChange={this.handleChange}
                     onSubmit={this.handleSubmit}
                     title="Customer's Store Credit" />
      </div>
    );
  }
}
