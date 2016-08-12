/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { connect } from 'react-redux';
import Api from 'lib/api';

import { FormField } from '../forms';
import CurrencyInput from '../forms/currency-input';
import MultiSelectRow from '../table/multi-select-row';
import Typeahead from '../typeahead/typeahead';
import SkuSuggestRow from './sku-suggest-row';

import { suggestSkus } from 'modules/products/details';
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
  isFetchingSkus: boolean|null,
  suggestSkus: (context: string, code: string) => Promise,
  suggestedSkus: Array<Sku>
};

type State = {
  sku: { [key:string]: string },
};

function mapStateToProps(state) {
  return {
    isFetchingSkus: _.get(state.asyncActions, 'products-suggestSkus.inProgress', null),
    suggestedSkus: _.get(state, 'products.details.suggestedSkus', []),
  };
}

class EditableSkuRow extends Component {
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

  @autobind
  suggestSkus(text) {
    return this.props.suggestSkus(this.props.skuContext, text);
  }

  @autobind
  handleSelectSku(sku) {
    this._skusTypeahead.clearState();
    this.handleUpdateCode(sku.code);
    this.handleUpdatePrice('salePrice', sku.price);
    // @TODO: get retailPrice directly from `sku` variable when this issue will be resolved:
    // https://github.com/FoxComm/green-river/issues/135
    Api.get(`/skus/${this.props.skuContext}/${sku.code}`).then(sku => {
      this.handleUpdatePrice('retailPrice', _.get(sku.attributes, 'retailPrice.v.value', 0));
    });
  }

  get skusMenu() {
    const items = this.props.suggestedSkus;
    if (_.isEmpty(items)) {
      return <div></div>;
    }

    return (
      <ul className="fc-typeahead__items">
        {items.map(sku => {
          return (
            <li
              className="fc-typeahead__item"
              onMouseDown={() => { this.handleSelectSku(sku) }}
              key={`item-${sku.id}`}
            >
              <SkuSuggestRow sku={sku} />
            </li>
          );
        })}
      </ul>
    );
  }

  skuCell(sku: Sku): Element {
    const code = _.get(this.props, 'sku.attributes.code.v');
    if (this.props.sku.feCode) {
      const value = this.state.sku.code || code;
      return (
        <FormField>
          <Typeahead
            ref={component => this._skusTypeahead = component}
            className="_no-search-icon"
            initialValue={value}
            onChange={this.handleUpdateCode}
            isFetching={this.props.isFetchingSkus}
            fetchItems={this.suggestSkus}
            itemsElement={this.skusMenu}
            minQueryLength={2}
            placeholder="SKU code"
            name="skuCode"
          />
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
  handleUpdateCode(value: string) {
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

export default connect(mapStateToProps, { suggestSkus })(EditableSkuRow);
