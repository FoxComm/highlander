
/* @flow weak */

import _ from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { autobind } from 'core-decorators';

import styles from './attrs-edit.css';

import { Dropdown } from '../dropdown';
import AppendInput from '../forms/append-input';
import ProductsQualifier from './qualifiers/products';

const offersItems = [
  ['orderPercentOff', 'Percent off order'],
  ['itemsSelectPercentOff', 'Percent off select items'],
  ['freeShipping', 'Free shipping'],
];

type Props = {
  onChange: (offer: Object) => any,
  discount: Object,
};

type Target = {
  value: any,
};

type Event = {
  target: Target,
};

export default class Offer extends Component {
  props: Props;

  static propTypes = {
    onChange: PropTypes.func.isRequired,
    discount: PropTypes.object,
  };


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
  handleDiscountChange(event: Event) {
    let discount = Number(event.target.value);
    discount = isNaN(discount) ? 0 : discount;
    discount = Math.min(100, discount);
    discount = Math.max(0, discount);

    this.setParams({
      discount,
    });
  }

  get percentOffOrder(): Element {
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
        <span>off your order.</span>
      </div>
    );
  }

  get percentOffItems(): Element {
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

  setParams(params: Object) {
    this.props.onChange({
      [this.offerType]: {
        ...this.offerParams,
        ...params,
      },
    });
  }

  setType(type: any) {
    this.props.onChange({
      [type]: this.offerParams,
    });
  }

  @autobind
  handleOfferTypeChange(value: any) {
    this.setType(value);
  }

  get content(): ?Element {
    switch (this.offerType) {
      case 'itemsSelectPercentOff':
        return this.itemsSelectPercentOff;
      default:
        return null;
    }
  }

  @autobind
  handleChangeReferences(references) {
    this.setParams({references});
  }

  get itemsSelectPercentOff(): Element {
    const references = _.get(this.offerParams, 'references', []);

    return (
      <ProductsQualifier
        label="Discount the items"
        references={references}
        onChange={this.handleChangeReferences}
      />
    );
  }

  render(): Element {
    return (
      <div>
        <div styleName="form-row">
          <Dropdown
            styleName="type-chooser"
            items={offersItems}
            value={this.offerType}
            onChange={this.handleOfferTypeChange}
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
