/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

// actions
import * as ProductActions from '../../modules/products/details';

// components
import ProductForm from './product-form';
import WaitAnimation from '../common/wait-animation';

// helpers
import { getProductAttributes, setProductAttribute } from '../../paragons/product';

// types
import type {
  FullProduct,
  ProductDetailsState,
  Variant,
} from '../../modules/products/details';

type Actions = {
  productAddAttribute: (field: string, type: string) => void,
  productNew: () => void,
  updateProduct: (product: FullProduct, context: ?string) => void,
};

type Props = {
  actions: Actions,
  products: ProductDetailsState,
};

export class NewProduct extends Component<void, Props, void> {
  static propTypes = {
    actions: PropTypes.shape({
      productAddAttribute: PropTypes.func.isRequired,
      productNew: PropTypes.func.isRequired,
      updateProduct: PropTypes.func.isRequired,
    }),

    products: PropTypes.shape({
      err: PropTypes.object,
      isFetching: PropTypes.bool,
      product: PropTypes.object,
    }),
  };

  componentDidMount() {
    this.props.actions.productNew();
  }

  render(): Element {
    let content = null;

    if (this.props.products.product) {
      content = (
        <ProductForm
          product={this.props.products.product}
          productId="new"
          title="New Product"
          onAddAttribute={this.props.actions.productAddAttribute}
          onSubmit={this.props.actions.updateProduct} />
      );
    }
    return <div className="fc-product">{content}</div>;
  }
}

function mapStateToProps(state) {
  return { products: state.products.details };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(ProductActions, dispatch) };
}

export default connect(mapStateToProps, mapDispatchToProps)(NewProduct);
