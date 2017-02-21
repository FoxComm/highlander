// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import hoistNonReactStatic from 'hoist-non-react-statics';

// data
import variantsReducer, { fetchProductVariants } from 'modules/product-variants/list';

import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  children: Element,
  // connected
  productVariants: Array<ProductVariant>,
  fetchProductVariants: (productId: number) => Promise,
  productVariantsState: AsyncState,
}

function mapLocalState(state) {
  return {
    productVariants: state.list,
    productVariantsState: _.get(state.asyncActions, 'fetchProductVariants', {}),
  };
}

function connectVariants(getProductId: (props: Object) => number) {
  return (WrappedComponent: ReactClass) => {
    const AssociatedVariants = class extends Component {
      props: Props;
      componentDidMount() {
        this.props.fetchProductVariants(getProductId(this.props));
      }

      render() {
        const props = _.omit(this.props, 'store');

        return <WrappedComponent {...props} />;
      }
    };

    const connected = _.flowRight(
      makeLocalStore(addAsyncReducer(variantsReducer)),
      connect(mapLocalState, { fetchProductVariants })
    )(AssociatedVariants);

    return hoistNonReactStatic(connected, WrappedComponent);
  };
}

export default connectVariants;


