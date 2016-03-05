/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

// actions
import * as ProductActions from '../../modules/products/details';

// components
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

// helpers
import Product from '../../paragons/product';

// types
import type { ProductDetailsState } from '../../modules/products/details';

type Actions = {
  fetchProduct: (id: number, context: ?string) => void,
};

type Params = {
  productId: string,
};

type Props = {
  actions: Actions,
  children: Object,
  params: Params,
  products: ProductDetailsState,
};

export class ProductPage extends Component<void, Props, void> {
  componentDidMount() {
    this.props.actions.fetchProduct(this.productId);
  }

  get productId(): number {
    return parseInt(this.props.params.productId);
  }

  get product(): ?Product {
    return this.props.products.product;
  }

  render(): Element {
    const { isFetching, product, err } = this.props.products;
    const productTitle: string = _.get(product, 'attributes.title', '');

    const showWaiting = isFetching || (!product && !err);
    const showError = !showWaiting && !product && err;

    let content = null;

    if (showWaiting) {
      content = <WaitAnimation />;
    } else if (showError) {
      content = <div>{_.get(err, 'status')}</div>;
    } else {
      content = (
        <div>
          <PageTitle title={productTitle} />
          <div>
            <SubNav productId={this.productId} product={this.product} />
            {this.props.children}
          </div>
        </div>
      );
    }

    return (
      <div className="fc-product">
        {content}
      </div>
    );
  }
}

function mapStateToProps(state) {
  return { products: state.products.details };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(ProductActions, dispatch) };
}


export default connect(mapStateToProps, mapDispatchToProps)(ProductPage);
