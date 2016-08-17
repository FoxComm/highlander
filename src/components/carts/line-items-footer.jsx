/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import SkuResult from './sku-result';
import Typeahead from 'components/typeahead/typeahead';

import { suggestSkus } from 'modules/skus/suggest';
import { updateLineItemCount } from 'modules/carts/details';

import type { Sku } from 'modules/skus/list';

const mapStateToProps = state => {
  return {
    suggestedSkus: _.get(state, 'skus.suggest.skus', []),
    isFetchingSkus: _.get(state.asyncActions, 'skus-suggest.inProgress', null),
  };
};

const mapDispatchToProps = { suggestSkus, updateLineItemCount };

type Props = {
  cart: {
    referenceNumber: string,
  },
  suggestedSkus: Array<Sku>,
  isFetchingSkus: boolean,
  suggestSkus: (code: string, context: ?string) => Promise,
  updateLineItemCount: Function,
};

export class CartLineItemsFooter extends Component {
  props: Props;

  @autobind
  currentQuantityForSku(sku: string): number {
    const skus = _.get(this.props, 'cart.lineItems.skus', []);
    const matched = _.find(skus, {skuCode: sku});
    return _.isEmpty(matched) ? 0 : matched.quantity;
  }

  @autobind
  skuSelected(item: Sku) {
    const { cart, updateLineItemCount } = this.props;
    const newQuantity = this.currentQuantityForSku(item.skuCode) + 1;
    updateLineItemCount(cart.referenceNumber, item.skuCode, newQuantity);
  }

  render() {
    const { props } = this;

    return (
      <div className="fc-line-items-add">
        <div className="fc-line-items-add-label">
          <strong>Add Item</strong>
        </div>
        <Typeahead
          onItemSelected={this.skuSelected}
          component={SkuResult}
          isFetching={props.isFetchingSkus}
          fetchItems={props.suggestSkus}
          items={props.suggestedSkus}
          placeholder="Product name or SKU..."
        />
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(CartLineItemsFooter);
