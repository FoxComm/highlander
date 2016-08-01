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
import * as ArchiveActions from '../../modules/products/archive';

// components
import { Dropdown, DropdownItem } from '../dropdown';
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import ErrorAlerts from 'components/alerts/error-alerts';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';
import ArchiveActionsSection from '../arcive-actions/archive-actions';

import styles from './page.css';

// helpers
import { transitionTo } from 'browserHistory';
import {
  setSkuAttribute,
} from '../../paragons/product';

// types
import type { Product } from 'paragons/product';

type Props = {
  actions: {
    createProduct: (product: Product) => void,
    fetchProduct: (productId: string, context: string) => void,
    productNew: () => void,
    updateProduct: (product: Product, context: string) => void,
  },
  children: any,
  params: { productId: string, context: string },
  products: {
    isFetching: boolean,
    isUpdating: boolean,
    product: ?Product,
    err: ?Object,
  },
  selectContextAvailable: boolean,
  archiveProduct: Function,
};

type State = {
  product: ?Product,
  context: string,
};

const SELECT_CONTEXT = [
  ['default', 'Default'],
  ['ru', 'Russian'],
];

/**
 * ProductPage represents the default layout of a product details page.
 * It displays the title, sub nav, and save button.
 */
class ProductPage extends Component {
  props: Props;

  static defaultProps = {
    selectContextAvailable: false,
  };

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

  get error(): ?Element {
    const { err } = this.props.products;
    if (err) {
      const message = _.get(err, ['messages', 0], 'There was an error saving the product.');
      return (
        <div styleName="error" className="fc-col-md-1-1">
          <ErrorAlerts error={message} />
        </div>
      );
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
    return _.get(product, 'attributes.title.v', '');
  }

  get selectContextDropdown() {
    if (this.props.selectContextAvailable) {
      return (
        <Dropdown onChange={this.handleContextChange}
                  value={this.props.params.context}
                  items={SELECT_CONTEXT} />
      );
    }
  }

  get titleActions(): Element {
    const { isUpdating } = this.props.products;

    return (
      <div className="fc-product-details__title-actions">
        { this.selectContextDropdown }
        <PrimaryButton
          className="fc-product-details__save-button"
          type="submit"
          disabled={isUpdating}
          isLoading={isUpdating}
          onClick={this.handleSubmit}>
          Save
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
  handleSetSkuProperty(code: string, field: string, type: string, value: string) {
    const { product } = this.state;
    if (product) {
      this.setState({
        product: setSkuAttribute(product, code, field, type, value),
      });
    }
  }

  @autobind
  handleUpdateProduct(product: Product) {
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

  renderArchiveActions() {
    return(
      <ArchiveActionsSection type="Product"
                             title={this.pageTitle}
                             archive={this.archiveProduct} />
    );
  }

  @autobind
  archiveProduct() {
    this.props.archiveProduct(this.props.params.productId).then(() => {
      transitionTo('products');
    });
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
      <div className="fc-product-details">
        <PageTitle title={this.pageTitle}>
          {this.titleActions}
        </PageTitle>
        <SubNav productId={this.props.params.productId} product={product} context={context}/>
        <div className="fc-grid">
          {this.error}
          <div className="fc-col-md-1-1">
            {children}
          </div>
        </div>

        {!this.isNew && this.renderArchiveActions()}
      </div>
    );
  }
}

export default connect(
  state => ({ products: state.products.details }),
  dispatch => ({
    actions: bindActionCreators(ProductActions, dispatch),
    ...bindActionCreators(ArchiveActions, dispatch),
  })
)(ProductPage);
