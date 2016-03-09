/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import CurrencyInput from '../forms/currency-input';
import MultiSelectRow from '../table/multi-select-row';

import type { IlluminatedSku } from '../../paragons/product';

type Column = {
  field: string,
  text: string,
};

type Props = {
  columns: Array<Column>,
  isNew: boolean,
  sku: IlluminatedSku,
  params: Object,
  updateField: (code: string, field: string, value: string) => void,
};

type State = {
  sku: { [key:string]: string },
};

export default class EditableSkuRow extends Component<void, Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);

    this.state = {
      sku: {}
    };
  }

  @autobind
  priceCell(sku: IlluminatedSku): Element {
    const value = this.state.sku.price || _.get(sku, 'attributes.price.value.value');
    return <CurrencyInput value={value} onChange={this.handleUpdatePrice} />;
  }

  get skuCell(): ?Element {
    if (!this.props.isNew) {
      return <div>{this.props.sku.code}</div>;
    }
  }

  @autobind
  setCellContents(sku: IlluminatedSku, field: string): any {
    switch(field) {
      case 'sku':
        return this.skuCell;
      case 'price':
        return this.priceCell(sku);
      default:
        return null;
    }
  }

  @autobind
  handleUpdatePrice(value: string) {
    this.setState(
      assoc(this.state, ['sku', 'price'], value),
      () => this.props.updateField(this.props.sku.code, 'price', value)
    );
  }

  render(): Element {
    const { columns, sku, params } = this.props;

    const key = `pdp-sku-${sku.code}`;
    return (
      <MultiSelectRow
        cellKeyPrefix={key}
        columns={columns}
        onClick={_.noop}
        row={sku}
        params={params}
        setCellContents={this.setCellContents} />
    );
  }
}
