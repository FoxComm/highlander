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
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import FoxyForm from '../forms/foxy-form';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

// helpers
import { getProductAttributes, setProductAttribute } from '../../paragons/product';

// types
import type { FullProduct, ProductDetailsState } from '../../modules/products/details';

type Actions = {
  fetchProduct: (id: number, context: ?string) => void,
  updateProduct: (product: FullProduct, context: ?string) => void,
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
  static propTypes = {
    children: PropTypes.node,

    actions: PropTypes.shape({
      fetchProduct: PropTypes.func.isRequired,
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

  get productId(): number {
    return parseInt(this.props.params.productId);
  }

  get product(): ?FullProduct {
    return this.props.products.product;
  }

  handleSubmit(productValues: { [key:string]: string }) {
    const product = this.product;
    if (product) {
      const updatedProduct = _.reduce(productValues, (res, val, key) => {
        return setProductAttribute(res, key, val);
      }, product);

      console.log(updatedProduct);
      this.props.actions.updateProduct(updatedProduct);
    }
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
    } else {
      content = (
        <FoxyForm onSubmit={this.handleSubmit.bind(this)}>
          <PageTitle title={productTitle}>
            <PrimaryButton type="submit">Save</PrimaryButton>
          </PageTitle>
          <div>
            <SubNav productId={this.productId} product={this.product} />
            {this.props.children}
          </div>
        </FoxyForm>
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
