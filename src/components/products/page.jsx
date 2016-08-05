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
import { Dropdown } from '../dropdown';
import { PageTitle } from '../section-title';
import { Button } from '../common/buttons';
import ButtonWithMenu from '../common/button-with-menu';
import ErrorAlerts from 'components/alerts/error-alerts';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';
import ArchiveActionsSection from '../archive-actions/archive-actions';

// styles
import styles from './page.css';

// helpers
import { isArchived } from 'paragons/common';
import { transitionTo } from 'browserHistory';
import {
  setSkuAttribute,
} from '../../paragons/product';
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';

// types
import type { Product } from 'paragons/product';

type Props = {
  actions: {
    createProduct: (product: Product) => void,
    fetchProduct: (productId: string, context: string) => Promise,
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
      this.props.actions.fetchProduct(this.props.params.productId, this.props.params.context)
        .then(({payload}) => {
          if (isArchived(payload)) transitionTo('products');
        });
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
    if (!err) return null;

    const message = _.get(err, ['messages', 0], 'There was an error saving the product.');
    return (
      <div styleName="error" className="fc-col-md-1-1">
        <ErrorAlerts error={message} />
      </div>
    );
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

  save() {
    let mayBeSaved = false;

    if (this.state.product) {
      if (this.isNew) {
        mayBeSaved = this.props.actions.createProduct(this.state.product, this.state.context);
      } else {
        mayBeSaved = this.props.actions.updateProduct(this.state.product, this.state.context);
      }
    }

    return mayBeSaved;
  }

  @autobind
  handleSubmit() {
    this.save();
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

  @autobind
  handleCancel(): void {
    transitionTo('products');
  }

  @autobind
  handleSelectSaving(value) {
    const mayBeSaved = this.save();
    if (!mayBeSaved) return;

    mayBeSaved.then(() => {
      switch (value) {
        case SAVE_COMBO.NEW:
          transitionTo('product-details', {
            productId: 'new',
            context: this.state.context,
          });
          this.props.actions.productNew();
          break;
        case SAVE_COMBO.DUPLICATE:
          transitionTo('product-details', {
            productId: 'new',
            context: this.state.context,
          });
          break;
        case SAVE_COMBO.CLOSE:
          transitionTo('products');
          break;
      }
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

    const { isUpdating } = this.props.products;

    return (
      <div className="fc-product-details">
        <PageTitle title={this.pageTitle}>
          { this.selectContextDropdown }
          <Button
            type="button"
            onClick={this.handleCancel}
            styleName="cancel-button">
            Cancel
          </Button>
          <ButtonWithMenu
            title="Save"
            menuPosition="right"
            onPrimaryClick={this.handleSubmit}
            onSelect={this.handleSelectSaving}
            isLoading={isUpdating}
            items={SAVE_COMBO_ITEMS}
          />
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
