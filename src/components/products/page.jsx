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
import { Dropdown, DropdownItem } from '../dropdown';
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

// helpers
import { transitionTo } from 'browserHistory';
import {
  getProductAttributes,
  setProductAttribute,
  setSkuAttribute,
} from '../../paragons/product';

// types
import type { FullProduct } from '../../modules/products/details';

type Props = {
  actions: {
    createProduct: (product: FullProduct) => void,
    fetchProduct: (productId: string, context: string) => void,
    productNew: () => void,
    updateProduct: (product: FullProduct, context: string) => void,
  },
  children: any,
  params: { productId: string, context: string },
  products: {
    isFetching: boolean,
    isUpdating: boolean,
    product: ?FullProduct,
    err: ?Object,
  }
};

type State = {
  product: ?FullProduct,
  context: string,
};

/**
 * ProductPage represents the default layout of a product details page.
 * It displays the title, sub nav, and save button.
 */
export class ProductPage extends Component {
  props: Props;

  state: State = {
    product: this.props.products.product,
    context: _.get(this.props.params, 'context', 'default'),
  };

  componentDidMount() {
    if (this.isNew) {
      this.props.actions.productNew();
    } else {
      this.props.actions.fetchProduct(this.props.params.productId, this.props.params.context);
    }
  }

  componentWillUnmount() {
    this.props.actions.productNew();
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

  get titleActions(): Element {
    const { isUpdating } = this.props.products;

    return (
      <div className="fc-product-details__title-actions">
        <Dropdown onChange={this.handleContextChange} value={this.props.params.context}>
          <DropdownItem value="default">Default</DropdownItem>
          <DropdownItem value="ru">Russian</DropdownItem>
        </Dropdown>
        <PrimaryButton
          className="fc-product-details__save-button"
          type="submit"
          disabled={isUpdating}
          isLoading={isUpdating}
          onClick={this.handleSubmit}>
          Save Draft
        </PrimaryButton>
      </div>
    );
  }

  @autobind
  handleContextChange(context: string) {
    const productId = this.props.params.productId;
    transitionTo('product-details', {
      productId: productId,
      context: context,
    });
    this.props.actions.fetchProduct(productId, context);
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
      });
    }
  }

  @autobind
  handleUpdateProduct(product: FullProduct) {
    this.setState({ product });
  }

  @autobind
  handleSubmit() {
    if (this.state.product) {
      if (this.isNew) {
        this.props.actions.createProduct(this.state.product, this.state.context);
      } else {
        this.props.actions.updateProduct(this.state.product, this.state.context);
      }
    }
  }

  render(): Element {
    const { product, context } = this.state;
    const { isFetching } = this.props.products;
    if (!product || isFetching) {
      return <div className="fc-product-details"><WaitAnimation /></div>;
    }

    const children = React.cloneElement(this.props.children, {
      ...this.props.children.props,
      onUpdateProduct: this.handleUpdateProduct,
      onSetSkuProperty: this.handleSetSkuProperty,
      product,
      entity: { entityId: this.props.params.productId, entityType: 'product' },
    });

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          {this.titleActions}
        </PageTitle>
        <SubNav productId={this.props.params.productId} product={product} context={context}/>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            {children}
          </div>
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({ products: state.products.details }),
  dispatch => ({ actions: bindActionCreators(ProductActions, dispatch) })
)(ProductPage);
