import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import formatCurrency from '../../../lib/format-currency';
import _ from 'lodash';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

import CurrencyInput from '../../forms/currency-input';
import DebitCredit from './debit-credit';
import { Form, FormField } from '../../forms';

function mapStateToProps(state) {
  return {
    paymentMethods: state.orders.paymentMethods,
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(PaymentMethodActions, dispatch),
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class NewGiftCard extends Component {
  static propTypes = {
    order: PropTypes.object.isRequired,
    paymentMethods: PropTypes.shape({
      isSearchingGiftCards: PropTypes.bool.isRequired,
      giftCards: PropTypes.array,
    }).isRequired,

    actions: PropTypes.shape({
      addOrderGiftCardPayment: PropTypes.func.isRequired,
      giftCardSearch: PropTypes.func.isRequired,
    }).isRequired,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      amountToUse: 0,
      giftCard: null,
      giftCardCode: null,
      showGiftCardSummary: false,
    };
  }

  componentWillReceiveProps(nextProps) {
    const gcResults = _.get(nextProps, 'paymentMethods.giftCards', []);
    const gcCode = _.get(gcResults, [0, 'code'], '');
    if (gcResults.length == 1 && _.startsWith(gcCode, this.state.giftCardCode)) {
      this.setState({
        giftCard: gcResults[0],
        giftCardCode: gcCode,
        showGiftCardSummary: true,
      });
    }
  }

  get availableBalance() {
    return _.get(this.state, 'giftCard.availableBalance', 0);
  }

  get giftCardSummary() {
    if (this.state.showGiftCardSummary) {
      return (
        <DebitCredit amountToUse={this.state.amountToUse}
                     availableBalance={this.availableBalance}
                     onChange={this.handleAmountToUseChange}
                     onSubmit={this.handleGiftCardSubmit} />
      );
    }
  }

  @autobind
  handleAmountToUseChange(value) {
    this.setState({
      amountToUse: Math.min(value, this.availableBalance),
    });
  }

  @autobind
  handleGiftCardChange({target}) {
    this.setState({
      giftCardCode: target.value,
    }, () => this.props.actions.giftCardSearch(target.value));
  }

  @autobind
  handleGiftCardSubmit(event) {
    event.preventDefault();
    this.props.actions.addOrderGiftCardPayment(
      this.props.order.referenceNumber,
      this.state.giftCardCode,
      this.state.amountToUse
    );
  }

  render() {
    return (
      <div className="fc-order-apply-gift-card">
        <Form className="fc-form-vertical">
          <FormField className="fc-order-apply-gift-card__card-number"
                     label="Gift Card Number">
            <input type="text"
                   name="giftCardCode"
                   onChange={this.handleGiftCardChange}
                   value={this.state.giftCardCode} />
          </FormField>
          {this.giftCardSummary}
        </Form>
      </div>
    );
  }
}
