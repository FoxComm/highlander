/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import SkuResult from './sku-result';
import Typeahead from 'components/typeahead/typeahead';
import { ApiErrors } from 'components/utils/errors';

import { suggestSkus } from 'modules/skus/suggest';
import { updateLineItemCount } from 'modules/carts/details';

import type { SuggestOptions } from 'modules/skus/suggest';

const mapStateToProps = state => {
  return {
    suggestedSkus: _.get(state, 'skus.suggest.skus', []),
    isFetchingSkus: _.get(state.asyncActions, 'skus-suggest.inProgress', null),
    updateLineItemErrors: _.get(state.asyncActions, 'updateLineItemCount.err', null),
  };
};

const mapDispatchToProps = { suggestSkus, updateLineItemCount };

type Props = {
  cart: {
    referenceNumber: string,
  },
  suggestedSkus: SkuSearch,
  isFetchingSkus: boolean,
  suggestSkus: (code: string, options?: SuggestOptions) => Promise<*>,
  updateLineItemCount: Function,
  updateLineItemErrors: Object,
};

export class CartLineItemsFooter extends Component {
  props: Props;

  @autobind
  skuSelected(item: SkuSearchItem) {
    const { cart: { referenceNumber }, updateLineItemCount } = this.props;

    const skus = _.get(this.props, 'cart.lineItems.skus', []);
    const matched = _.find(skus, { sku: item.skuCode });

    if (!_.isEmpty(matched)) {
      return;
    }

    updateLineItemCount(referenceNumber, item.skuCode, 1);
  }

  @autobind
  suggestSkus(value: string): Promise<*> {
    return this.props.suggestSkus(value, {
      useTitle: true,
      omitArchived: true,
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

        {updateLineItemErrors && <ApiErrors className="fc-line-items-errors" response={updateLineItemErrors} />}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(CartLineItemsFooter);
