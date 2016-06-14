/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import { FormField } from '../forms';
import CurrencyInput from '../forms/currency-input';
import MultiSelectRow from '../table/multi-select-row';

import type { Sku } from '../../modules/skus/details';

type Column = {
  field: string,
  text: string,
};

type Props = {
  columns: Array<Column>,
  sku: Sku,
  params: Object,
  updateField: (code: string, field: string, value: string) => void,
};

type State = {
  sku: { [key:string]: string },
};

export default class EditableSkuRow extends Component<void, Props, State> {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { sku: {} };
  }

  @autobind
  priceCell(sku: Sku, field: string): Element {
    const value = _.get(this.state.sku, field) || _.get(sku, ['attributes', field, 'v', 'value']);
    const onChange = (value) => this.handleUpdatePrice(field, value);
    return (
      <div className="fc-editable-sku-row__price">
        <CurrencyInput value={value} onChange={onChange} />
      </div>
    );
  }

  @autobind
  upcCell(sku: Sku): Element {
    const value = this.state.sku.upc || _.get(sku, 'attributes.upc.v');
    return (
      <FormField>
        <input type="text" value={value} onChange={this.handleUpdateUpc} />
      </FormField>
    );
  }

  get code(): string {
    return this.props.sku.code || this.props.sku.feCode;
  }

  skuCell(sku: Sku): Element {
    if (this.props.sku.code && this.props.sku.createdAt) {
      return <div>{this.props.sku.code}</div>;
    } else {
      const value = this.state.sku.code || this.props.sku.code;
      return (
        <FormField>
          <input type="text" value={value} onChange={this.handleUpdateCode} required />
        </FormField>
      );
    }
  }

  @autobind
  setCellContents(sku: Sku, field: string): any {
    switch(field) {
      case 'sku':
        return this.skuCell(sku);
      case 'retailPrice':
      case 'salePrice':
        return this.priceCell(sku, field);
      case 'upc':
        return this.upcCell(sku);
      default:
        return null;
    }
  }

  @autobind
  handleUpdateCode({ target }: Object) {
    const value = target.value;
    this.setState(
      assoc(this.state, ['sku', 'code'], value),
      () => this.props.updateField(this.code, 'code', 'string', value)
    );
  }

  @autobind
  handleUpdatePrice(field: string, value: string) {
    this.setState(
      assoc(this.state, ['sku', field], value),
      () => this.props.updateField(this.code, field, 'price', value)
    );
  }

  @autobind
  handleUpdateUpc({target}: Object) {
    const value = target.value;

    this.setState(
      assoc(this.state, ['sku', 'upc'], value),
      () => this.props.updateField(this.code, 'upc', 'string', value)
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
