
/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import { Dropdown } from '../dropdown';
import CurrencyInput from '../forms/currency-input';

import styles from './qualifier-type.css';

const qualifierItems = [
  ['orderAny', 'Order - No qualifier'],
  ['orderTotalAmount', 'Order - Total amount of order'],
  ['itemsAny', 'Items - No qualifier']
];

type Qualifier = {
  [type:string]: Object;
}

type Props = {
  onChange: (qualifier: Qualifier) => any;
  discount: Object;
};

export default class QualifierType extends Component {
  props: Props;

  get qualifier() {
    const { discount } = this.props;
    return _.get(discount, 'form.attributes.qualifier', {});
  }

  get qualifierType() {
    return Object.keys(this.qualifier)[0];
  }

  get qualifierParams() {
    return this.qualifier[this.qualifierType] || {};
  }

  get controlAfterType() {
    switch (this.qualifierType) {
      case 'orderTotalAmount':
        return this.totalAmountWidget;
      default:
        return null;
    }
  }

  setParams(params) {
    this.props.onChange({
      [this.qualifierType]: params,
    });
  }

  setType(type) {
    this.props.onChange({
      [type]: this.qualifierParams,
    });
  }


  get totalAmountWidget() {
    return (
      <div styleName="control-after-type">
        Spend&nbsp;
        <CurrencyInput
          styleName="total-amount"
          value={this.qualifierParams.totalAmount}
          onChange={this.handleTotalAmountChange}
        />
        &nbsp;or more.
      </div>
    );
  }

  @autobind
  handleTotalAmountChange(value) {
    this.setParams({
      totalAmount: value,
    });
  }


  @autobind
  handleQualifierTypeChange(value) {
    this.setType(value);
  }

  render() {
    return (
      <div>
        <Dropdown
          styleName="qualifier-types"
          items={qualifierItems}
          value={this.qualifierType}
          onChange={this.handleQualifierTypeChange}
        />
        {this.controlAfterType}
      </div>
    );
  }
}
