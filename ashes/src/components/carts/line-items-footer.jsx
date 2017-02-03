/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import SkuResult from './sku-result';
import Typeahead from 'components/typeahead/typeahead';
import ErrorAlerts from '../alerts/error-alerts';

import { suggestSkus } from 'modules/product-variants/suggest';
import { updateLineItemCount } from 'modules/carts/details';

import type { SuggestOptions } from 'modules/product-variants/suggest';

import type { ProductVariant } from 'modules/product-variants/list';

const mapStateToProps = state => {
  return {
    suggestedSkus: _.get(state, 'skus.suggest.skus', []),
    isFetchingSkus: _.get(state.asyncActions, 'skus-suggest.inProgress', null),
    updateLineItemErrors: _.get(state.asyncActions, 'updateLineItemCount.err.response.body.errors', null),
  };
};

const mapDispatchToProps = { suggestSkus, updateLineItemCount };

type Props = {
  cart: {
    referenceNumber: string,
  },
  suggestedSkus: Array<ProductVariant>,
  isFetchingSkus: boolean,
  suggestSkus: (code: string, options?: SuggestOptions) => Promise,
  updateLineItemCount: Function,
  updateLineItemErrors: Array<string>
};

export class CartLineItemsFooter extends Component {
  props: Props;

  @autobind
  skuSelected(item: ProductVariant) {
    const { cart: { referenceNumber }, updateLineItemCount } = this.props;

    const skus = _.get(this.props, 'cart.lineItems.skus', []);
    const matched = _.find(skus, { sku: item.skuCode });

    if (!_.isEmpty(matched)) {
      return;
    }

    updateLineItemCount(referenceNumber, item.skuCode, 1);
  }

  @autobind
  suggestSkus(value: string): Promise {
    return this.props.suggestSkus(value, {
      useTitle: true,
    });
  }

  render() {
    const { updateLineItemErrors, isFetchingSkus, suggestedSkus } = this.props;

    return (
      <div className="fc-line-items-add">
        <div className="fc-line-items-add-label">
          <strong>Add Item</strong>
        </div>
        <Typeahead
          onItemSelected={this.skuSelected}
          component={SkuResult}
          isFetching={isFetchingSkus}
          fetchItems={this.suggestSkus}
          items={suggestedSkus}
          placeholder="Product name or SKU..."
        />

        {updateLineItemErrors && <ErrorAlerts errors={updateLineItemErrors} />}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(CartLineItemsFooter);
