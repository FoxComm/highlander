/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import SkuResult from './sku-result';
import Typeahead from 'components/typeahead/typeahead';

import { suggestSkus } from 'modules/skus/suggest';
import { updateLineItemCount } from 'modules/carts/details';

import type { SuggestOptions } from 'modules/skus/suggest';

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
  suggestSkus: (code: string, options?: SuggestOptions) => Promise,
  updateLineItemCount: Function,
};

export class CartLineItemsFooter extends Component {
  props: Props;

  @autobind
  skuSelected(item: Sku) {
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
          fetchItems={this.suggestSkus}
          items={props.suggestedSkus}
          placeholder="Product name or SKU..."
        />
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(CartLineItemsFooter);
