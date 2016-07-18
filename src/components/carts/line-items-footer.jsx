/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import { collectLineItems } from 'paragons/order';

import SkuResult from './sku-result';
import Typeahead from 'components/typeahead/typeahead';

import * as skuSearchActions from 'modules/carts/sku-search';

const mapStateToProps = state => {
  return {
    skuSearch: state.carts.skuSearch,
  };
};

const mapDispatchToProps = { ...skuSearchActions };

type Sku = {
  code: string,
};

type SkuItem = {
  sku: string,
};

type Props = {
  cart: {
    referenceNumber: string,
  },
  skuSearch: {
    phrase: string,
    results: {
      isFetching: boolean,
      rows: Array<Sku>,
    },
  },
  suggestSkus: Function,
  updateCount: Function,
};

@connect(mapStateToProps, mapDispatchToProps)
export default class CartLineItemsFooter extends Component {
  props: Props;

  @autobind
  currentQuantityForSku(sku: string): number {
    const items = _.get(this.props, 'cart.lineItems.skus', []);
    const skus = collectLineItems(items);
    const matched = skus.find((o) => { return o.sku === sku;});
    return _.isEmpty(matched) ? 0 : matched.quantity;
  }

  @autobind
  skuSelected(item: Sku) {
    const { cart, updateCount } = this.props;
    const newQuantity = this.currentQuantityForSku(item.code) + 1;
    updateCount(cart, item.code, newQuantity);
  }

  render() {
    const suggestedSkus = _.get(this.props, 'skuSearch.results.rows', []);
    const isFetching = _.get(this.props, 'skuSearch.results.isFetching', false);
    const query = _.get(this.props, 'skuSearch.phrase', '');
    return (
      <div className="fc-line-items-add">
        <div className="fc-line-items-add-label">
          <strong>Add Item</strong>
        </div>
        <Typeahead onItemSelected={this.skuSelected}
                   component={SkuResult}
                   isFetching={isFetching}
                   fetchItems={this.props.suggestSkus}
                   items={suggestedSkus}
                   query={query}
                   placeholder="Product name or SKU..."/>
      </div>
    );
  }
}
