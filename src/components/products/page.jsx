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
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

// helpers
import {
  getProductAttributes,
  setProductAttribute,
  setSkuAttribute,
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
    } else {
      this.props.actions.productNew();
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    const { isFetching, isUpdating } = nextProps.products;
    if (!isFetching && !isUpdating) {
      this.setState({ product: nextProps.products.product });
    }
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
    const { product } = this.state;
    if (product) {
      this.setState({
        product: setSkuAttribute(product, code, field, value),
      })
    }
  }

  @autobind
  handleSubmit() {
    if (this.isNew) {
      this.props.actions.createProduct(this.state.product);
    } else {
      this.props.actions.updateProduct(this.state.product);
    }
  }

  render(): Element {
    const { product } = this.state;
    const { isFetching, isUpdating } = this.props.products;
    if (!product || isFetching) {
      return <div className="fc-product-details"><WaitAnimation /></div>;
    }

    const children = React.cloneElement(this.props.children, {
      onSetProperty: this.handleSetAttribute,
      onSetSkuProperty: this.handleSetSkuProperty,
      product,
    });

    const wait = isUpdating ? <WaitAnimation /> : null;

    return (
      <Form onSubmit={this.handleSubmit}>
        <PageTitle title={this.pageTitle}>
          <PrimaryButton 
            className="fc-product-details__save-button" 
            type="submit" 
            disabled={isUpdating}>
            Save Draft {wait}
          </PrimaryButton>
        </PageTitle>
        <div>
          <SubNav productId={this.props.params.productId} product={product} />
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
