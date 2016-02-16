import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import formatCurrency from '../../../lib/format-currency';

import CurrencyInput from '../../forms/currency-input';
import { Form, FormField } from '../../forms';

class ApplyGiftCard extends Component {
  static propTypes = {
    availableBalance: PropTypes.number,
  };

  static defaultProps = {
    availableBalance: 10000,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      amountToUse: 0,
      giftCardNumber: null,
    };
  }

  get availableBalance() {
    return formatCurrency(this.props.availableBalance);
  }

  get futureAvailableBalance() {
    return formatCurrency(this.props.availableBalance - this.state.amountToUse);
  }

  @autobind
  handleAmountToUseChange(value) {
    if (value <= this.props.availableBalance) {
      this.setState({ amountToUse: value });
    }
  }

  @autobind
  handleGiftCardChange({target}) {
    this.setState({ giftCardNumber: target.value });
  }

  render() {
    return (
      <div className="fc-order-apply-gift-card">
        <Form className="fc-form-vertical">
          <FormField className="fc-order-apply-gift-card__card-number"
                     label="Gift Card Number">
            <input type="text"
                   name="giftCardNumber"
                   onChange={this.handleGiftCardChange}
                   value={this.state.giftCardNumber} />
          </FormField>
        </Form>
        <div className="fc-grid fc-grid-no-gutter">
          <div className="fc-order-apply-gift-card__statistic fc-col-md-1-4">
            <div className="fc-order-apply-gift-card__statistic-label">
              Available Balance
            </div>
            <div className="fc-order-apply-gift-card__statistic-value">
              {this.availableBalance}
            </div>
          </div>
          <FormField className="fc-order-apply-gift-card__amount-form fc-col-md-1-4"
                     label="Amount to Use"
                     labelClassName="fc-order-apply-gift-card__amount-form-value">
            <CurrencyInput onChange={this.handleAmountToUseChange}
                           value={this.state.amountToUse} />
          </FormField>
          <div className="fc-order-apply-gift-card__statistic fc-col-md-1-4">
            <div className="fc-order-apply-gift-card__statistic-label">
              Future Available Balance
            </div>
            <div className="fc-order-apply-gift-card__statistic-value">
              {this.futureAvailableBalance}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default ApplyGiftCard;
