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
import { Form } from '../forms';
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import WaitAnimation from '../common/wait-animation';

// helpers
import {
  getProductAttributes,
  setProductAttribute,
} from '../../paragons/product';

// types
import type { FullProduct } from '../../modules/products/details';

type Props = {
  actions: {
    fetchProduct: (productId: string) => void,
  },
  children: any,
  params: { productId: string },
  products: {
    isFetching: boolean,
    isUpdating: boolean,
    product: ?FullProduct,
    err: ?Object,
  }
};

type State = {
  product: ?FullProduct,
};

/**
 * ProductPage represents the default layout of a product details page.
 * It displays the title, sub nav, and save button.
 */
export class ProductPage extends Component<void, Props, State> {
  static propTypes = {
    actions: PropTypes.shape({
      fetchProduct: PropTypes.func.isRequired,
    }).isRequired,

    children: PropTypes.node,

    params: PropTypes.shape({
      productId: PropTypes.string.isRequired,
    }).isRequired,

    products: PropTypes.shape({
      isFetching: PropTypes.bool.isRequired,
      isUpdating: PropTypes.bool.isRequired,
      product: PropTypes.object,
      err: PropTypes.object,
    }).isRequired,
  };

  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { product: this.props.products.product };
  }

  componentDidMount() {
    if (!this.isNew) {
      this.props.actions.fetchProduct(this.props.params.productId);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState({ product: nextProps.products.product });
  }

  get isNew(): boolean {
    return this.props.params.productId === 'new';
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Product';
    }

    const { product } = this.props.products;
    const attributes = product ? getProductAttributes(product) : {};
    return _.get(attributes, 'title.value', '');
  }

  @autobind
  handleSetAttribute(field: string, type: string, value: string) {
    const { product } = this.state;
    if (product) {
      this.setState({
        product: setProductAttribute(product, field, type, value),
      });
    }
  }

  @autobind
  handleSetSkuProperty(code: string, field: string, type: string, value: string) {
    console.log('update sku not implemented');
  }

  @autobind
  handleSubmit() {
    console.log('not implemented');
  }

  render(): Element {
    const { product } = this.state;
    if (!product) {
      return <div className="fc-product-details"><WaitAnimation /></div>;
    }

    const children = React.cloneElement(this.props.children, {
      onSetProperty: this.handleSetAttribute,
      onSetSkuProperty: this.handleSetSkuProperty,
      product,
    });

    return (
      <Form onSubmit={this.handleSubmit}>
        <PageTitle title={this.pageTitle}>
          <PrimaryButton>Save Draft</PrimaryButton>
        </PageTitle>
        <div>
          {children}
        </div>
      </Form>
    );
  }
}

export default connect(
  state => ({ products: state.products.details }),
  dispatch => ({ actions: bindActionCreators(ProductActions, dispatch) })
)(ProductPage);
