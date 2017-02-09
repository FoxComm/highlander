// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';

// components
import AssociatedList from './associated-list';
import { IndexLink } from '../link';

// data
import variantsReducer, { fetchProductVariants } from 'modules/product-variants/list';

import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  productId: number,
  // connected
  list: Array<ProductVariant>,
  fetchProductVariants: (productId: number) => Promise,
  fetchState: AsyncState,
}

function mapLocalState(state) {
  return {
    list: state.list,
    fetchState: _.get(state.asyncActions, 'fetchProductVariants', {}),
  };
}

class AssociatedVariants extends Component {
  props: Props;
  componentDidMount() {
    this.props.fetchProductVariants(this.props.productId);
  }

  render() {
    const { props } = this;

    const list = _.map(props.list, (variant: ProductVariant) => {
      // @TODO: backend should include option string in variant.title
      const title = (
        <IndexLink to="product-variant" params={{productVariantId: variant.variantId}}>
          {variant.title}
        </IndexLink>
      );
      return {
        image: variant.image,
        title,
        subtitle: variant.skuCode
      };
    });

    return (
      <AssociatedList
        title="Associated Variants"
        list={list}
        fetchState={props.fetchState}
      />
    );
  }
}

export default _.flowRight(
  makeLocalStore(addAsyncReducer(variantsReducer)),
  connect(mapLocalState, { fetchProductVariants })
)(AssociatedVariants);


