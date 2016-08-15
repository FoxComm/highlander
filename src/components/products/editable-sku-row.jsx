/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';

import { FormField } from '../forms';
import CurrencyInput from '../forms/currency-input';
import MultiSelectRow from '../table/multi-select-row';
import Typeahead from '../typeahead/typeahead';

import type { Sku } from 'modules/skus/details';

type Column = {
  field: string,
  text: string,
};

type Props = {
  columns: Array<Column>,
  sku: Sku,
  params: Object,
  skuContext: string,
  updateField: (code: string, field: string, value: string) => void,
};

type State = {
  sku: { [key:string]: string },
};

function suggestSkus(code, context) {
  return post('sku_search_view/_search', dsl.query({
    bool: {
      filter: [
        dsl.termFilter('context', context),
      ],
      must: [
        dsl.matchQuery('code', {
          query: code,
          type: 'phrase'
        }),
      ]
    },
  }));
}

export default class EditableSkuRow extends Component {
  props: Props;

  state: State = {
    sku: {},
  };

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
    if (this.props.sku.code) {
      return this.props.sku.code;
    }

    return this.props.sku.feCode || 'new';
  }

  skuCell(sku: Sku): Element {
    const code = _.get(this.props, 'sku.attributes.code.v');
    if (this.props.sku.feCode) {
      const value = this.state.sku.code || code;
      return (
        <FormField>
          <input type="text" value={value} onChange={this.handleUpdateCode} required />
        </FormField>
      );
    }

    return <div>{code}</div>;
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
    suggestSkus(value, this.props.skuContext).then(response => console.log('got', response));
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

    return (
      <MultiSelectRow
        columns={columns}
        row={sku}
        params={params}
        setCellContents={this.setCellContents} />
    );
  }
}
