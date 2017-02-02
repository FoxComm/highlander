/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';

import SkuResult from './sku-result';
import Typeahead from 'components/typeahead/typeahead';
import ErrorAlerts from '../alerts/error-alerts';

import reducer, { suggestVariants } from 'modules/product-variants/suggest';
import { updateLineItemCount } from 'modules/carts/details';

import type { SuggestOptions } from 'modules/product-variants/suggest';
import type { ProductVariant } from 'modules/product-variants/list';

function mapLocalStateToProps(state) {
  return {
    isFetchingVariants: _.get(state.asyncActions, 'suggestVariants.inProgress', false),
    suggestedVariants: _.get(state, 'variants', []),
  };
}

const mapGlobalStateToProps = state => {
  return {
    updateLineItemErrors: _.get(state.asyncActions, 'updateLineItemCount.err.response.body.errors', null),
  };
};

type Props = {
  cart: {
    referenceNumber: string,
  },
  suggestedVariants: Array<ProductVariant>,
  isFetchingVariants: boolean,
  suggestVariants: (code: string, options?: SuggestOptions) => Promise,
  updateLineItemCount: Function,
  updateLineItemErrors: Array<string>,
};

export class CartLineItemsFooter extends Component {
  props: Props;

  @autobind
  variantSelected(item: ProductVariant) {
    const { cart: { referenceNumber }, updateLineItemCount } = this.props;

    const skus = _.get(this.props, 'cart.lineItems.skus', []);
    const matched = _.find(skus, { productVariantId: item.id });

    if (!_.isEmpty(matched)) {
      return;
    }

    updateLineItemCount(referenceNumber, item.id, 1);
  }

  @autobind
  suggestVariants(value: string): Promise {
    return this.props.suggestVariants(value, {
      useTitle: true,
    });
  }

  render() {
    const { updateLineItemErrors, isFetchingVariants, suggestedVariants } = this.props;

    return (
      <div className="fc-line-items-add">
        <div className="fc-line-items-add-label">
          <strong>Add Item</strong>
        </div>
        <Typeahead
          onItemSelected={this.variantSelected}
          component={SkuResult}
          isFetching={isFetchingVariants}
          fetchItems={this.suggestVariants}
          items={suggestedVariants}
          placeholder="Product name or SKU..."
        />

        {updateLineItemErrors && <ErrorAlerts errors={updateLineItemErrors} />}
      </div>
    );
  }
}

export default _.flowRight(
  connect(mapGlobalStateToProps, { updateLineItemCount }),
  makeLocalStore(addAsyncReducer(reducer)),
  connect(mapLocalStateToProps, { suggestVariants }),
)(CartLineItemsFooter);
