
/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import { Dropdown } from '../dropdown';
import CurrencyInput from '../forms/currency-input';
import ProductsQualifier from './qualifiers/products';

import styles from './attrs-edit.css';

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
        <span>Spend</span>
        <CurrencyInput
          styleName="inline-edit-input"
          value={this.qualifierParams.totalAmount}
          onChange={this.handleTotalAmountChange}
        />
        <span>or more.</span>
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

  @autobind
  handleChangeReferences(references) {
    this.setParams({references});
  }

  get itemsAnyQualifier() {
    const references = _.get(this.qualifierParams, 'references', []);

    return (
      <ProductsQualifier
        label="Items for qualify"
        references={references}
        onChange={this.handleChangeReferences}
      />
    );
  }

  get content() {
    switch (this.qualifierType) {
      case 'itemsAny':
        return this.itemsAnyQualifier;
      default:
        return null;
    }
  }

  render() {
    return (
      <div>
        <div styleName="form-row">
          <Dropdown
            styleName="type-chooser"
            items={qualifierItems}
            value={this.qualifierType}
            onChange={this.handleQualifierTypeChange}
          />
          {this.controlAfterType}
        </div>
        <div styleName="form-row">
          {this.content}
        </div>
      </div>
    );
  }
}
