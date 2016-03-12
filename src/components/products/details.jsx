/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
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
import type { FullProduct, ProductDetailsState } from '../../modules/products/details';

type Actions = {
  fetchProduct: (id: string, context: ?string) => void,
  productAddAttribute: (field: string, type: string) => void,
  updateProduct: (product: FullProduct, context: ?string) => void,
};

type Params = {
  productId: string,
};

type Props = {
  actions: Actions,
  params: Params,
  products: ProductDetailsState,
};

export class ProductDetails extends Component<void, Props, void> {
  static propTypes = {
    actions: PropTypes.shape({
      fetchProduct: PropTypes.func.isRequired,
      productAddAttribute: PropTypes.func.isRequired,
      updateProduct: PropTypes.func.isRequired,
    }),

    params: PropTypes.shape({
      productId: PropTypes.string.isRequired,
    }),

    products: PropTypes.shape({
      err: PropTypes.object,
      isFetching: PropTypes.bool,
      product: PropTypes.object,
    }),
  };

  componentDidMount() {
    this.props.actions.fetchProduct(this.productId);
  }

  get productId(): string{
    return this.props.params.productId;
  }

  render(): Element {
    const { isFetching, product, err } = this.props.products;
    const attributes = product ? getProductAttributes(product) : {};
    const productTitle: string = _.get(attributes, 'title.value', '');

    const showWaiting = isFetching || (!product && !err);
    const showError = !showWaiting && !product && err;

    let content = null;

    if (showWaiting) {
      content = <WaitAnimation />;
    } else if (showError) {
      content = <div>{_.get(err, 'status')}</div>;
    } else if (product) {
      content = (
        <ProductForm
          product={product}
          productId={this.productId}
          title={productTitle}
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


export default connect(mapStateToProps, mapDispatchToProps)(ProductDetails);
