
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import { Dropdown } from '../dropdown';
import CurrencyInput from '../forms/currency-input';

import styles from './qualifier-type.css';

const qualifierItems = [
  [null, 'Order - No qualifier'],
  ['orderTotalAmount', 'Order - Total amount of order'],
];

export default class QualifierType extends Component {

  state = {
    qualifierType: null,
    totalAmount: 10000,
  };

  get controlAfterType() {
    switch (this.state.qualifierType) {
      case 'orderTotalAmount':
        return this.totalAmountWidget;
      default:
        return null;
    }
  }

  @autobind
  handleTotalAmountChange(value) {
    this.setState({
      totalAmount: value,
    });
  }

  get totalAmountWidget() {
    return (
      <div styleName="control-after-type">
        Spend&nbsp;
        <CurrencyInput
          styleName="total-amount"
          value={this.state.totalAmount}
          onChange={this.handleTotalAmountChange}
        />
        &nbsp;or more.
      </div>
    );
  }

  @autobind
  handleQualifierTypeChange(value) {
    this.setState({
      qualifierType: value,
    });
  }

  render() {
    return (
      <div>
        <Dropdown
          styleName="qualifier-types"
          items={qualifierItems}
          value={this.state.qualifierType}
          onChange={this.handleQualifierTypeChange}
        />
        {this.controlAfterType}
      </div>
    );
  }
}
