import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as CartActions from 'modules/carts/details';
import * as StoreCreditTotalsActions from 'modules/customers/store-credit-totals';

import DebitCredit from 'components/payment-row/debit-credit';

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
      ...bindActionCreators(CartActions, dispatch),
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
    totals: PropTypes.object,
    cancelAction: PropTypes.func.isRequired,

    actions: PropTypes.shape({
      setStoreCreditPayment: PropTypes.func.isRequired,
      fetchTotals: PropTypes.func.isRequired,
    }).isRequired,
  };

  componentDidMount() {
    this.props.actions.fetchTotals();
  }

  get availableBalance() {
    return _.get(this.props, 'totals.totals.availableBalance', 0);
  }

  @autobind
  handleSubmit(amountToUse) {
    this.props.actions.setStoreCreditPayment(
      this.props.order.referenceNumber,
      amountToUse
    );
  }

  render() {
    return (
      <div className="fc-order-apply-store-credit">
        <DebitCredit
          availableBalance={this.availableBalance}
          onCancel={this.props.cancelAction}
          onSubmit={this.handleSubmit}
          title="Customer's Store Credit"
        />
      </div>
    );
  }}
