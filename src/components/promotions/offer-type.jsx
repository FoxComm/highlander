/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import styles from './attrs-edit.css';

import { Dropdown } from '../dropdown';
import CurrencyInput from '../forms/currency-input';
import AppendInput from '../forms/append-input';
import { Checkbox } from '../checkbox/checkbox';

const offersItems = [
  ['orderPercentOff', 'Percent off order'],
  ['itemsSelectPercentOff', 'Percent off select items'],
  ['freeShipping', 'Free shipping'],
];

type Props = {
  onChange: (offer: Object) => any;
  discount: Object;
};

export default class Offer extends Component {
  props: Props;

  get offer() {
    const { discount } = this.props;
    return _.get(discount, 'form.attributes.offer', {});
  }

  get offerType() {
    return Object.keys(this.offer)[0];
  }

  get offerParams() {
    return this.offer[this.offerType] || {};
  }

  get controlAfterType() {
    switch (this.offerType) {
      case 'orderPercentOff':
        return this.percentOffOrder;
      case 'itemsSelectPercentOff':
        return this.percentOffItems;
      default:
        return null;
    }
  }

  @autobind
  handleDiscountChange({target}) {
    let discount = Number(target.value);
    discount = isNaN(discount) ? 0 : discount;
    discount = Math.min(100, discount);
    discount = Math.max(0, discount);

    this.setParams({
      discount,
    });
  }

  get percentOffOrder() {
    return (
      <div styleName="control-after-type">
        <span>Get</span>
        <AppendInput
          styleName="inline-edit-input"
          min={0}
          max={100}
          type="number"
          plate="%"
          value={this.offerParams.discount}
          onChange={this.handleDiscountChange}
        />
        <span>or more.</span>
      </div>
    );
  }

  get percentOffItems() {
    return (
      <div styleName="control-after-type">
        <span>Get</span>
        <AppendInput
          styleName="inline-edit-input"
          min={0}
          max={100}
          type="number"
          plate="%"
          value={this.offerParams.discount}
          onChange={this.handleDiscountChange}
        />
        <span>off discounted items.</span>
      </div>
    );
  }

  setParams(params) {
    this.props.onChange({
      [this.offerType]: params,
    });
  }

  setType(type) {
    this.props.onChange({
      [type]: this.offerParams,
    });
  }

  @autobind
  handleOfferTypeChange(value) {
    this.setType(value);
  }

  get content() {
    switch (this.offerType) {
      case 'itemsSelectPercentOff':
        return this.itemsSelectPercentOff;
      default:
        return null;
    }
  }

  get itemsSelectPercentOff() {
    return (
      <div>
        <div styleName="header">Discounted Items</div>
        <Checkbox styleName="attr-row">Exclude gift cards</Checkbox>
        <Checkbox styleName="attr-row">Same as qualifying items</Checkbox>
      </div>
    );
  }

  render() {
    return (
      <div>
        <Dropdown
          styleName="type-chooser"
          items={offersItems}
          value={this.offerType}
          onChange={this.handleOfferTypeChange}
        />
        {this.controlAfterType}
        {this.content}
      </div>
    );
  }
}
