/**
 * @flow weak
 * This component handles a product prepartion for Amazon
 * The product itself is in local state, and all modifications goes through it
 * The product passes by props to the descendant components, but modifies only in
 * immutable way though handlers
 * <Form> component is used as a wrapper to use its validation mechanism
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import classNames from 'classnames';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// actions
import { transitionTo } from 'browserHistory';
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/channels/amazon';
import * as schemaActions from 'modules/object-schema';

// components
import WaitAnimation from 'components/common/wait-animation';
import ContentBox from 'components/content-box/content-box';
import { Button, PrimaryButton } from 'components/common/buttons';
import SaveCancel from 'components/common/save-cancel';
import Form from 'components/forms/form';
import ProductAmazonMain from './product-amazon-main';
import ProductAmazonVariants from './product-amazon-variants';
import type { Product } from 'paragons/product';

// selectors
import { getSuggest, cat } from './selector';

// styles
import s from './product-amazon.css';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({
      fetchSchema: schemaActions.fetchSchema,
      updateProduct: productActions.updateProduct,
      fetchProduct: productActions.fetchProduct,
      clearAmazonErrors: amazonActions.clearErrors,
      resetAmazonState: amazonActions.resetState,
      fetchAmazonSchema: amazonActions.fetchAmazonSchema,
      pushProduct: amazonActions.pushToAmazon,
      fetchProductStatus: amazonActions.fetchAmazonProductStatus,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const originalProduct = state.products.details.product;
  const { suggest, schema, productStatus, credentials } = state.channels.amazon;

  return {
    originalProduct,
    amazonEnabled: !!credentials,
    productStatus,
    fetchingProduct: _.get(state.asyncActions, 'fetchProduct.inProgress'),
    fetchingSchema: _.get(state.asyncActions, 'fetchAmazonSchema.inProgress'),
    schema,
  };
}

type State = {
  product: ?Product,
  error: any,
  saveBtnIsLoading: boolean,
};

class ProductAmazon extends Component {
  state: State = {
    product: this.props.originalProduct,
    error: '',
    saveBtnIsLoading: false,
  };

  componentDidMount() {
    const { productId } = this.props.params;
    const { originalProduct } = this.props;
    const categoryId = this.nodeId();
    const {
      clearAmazonErrors,
      fetchProduct,
      fetchProductStatus,
      fetchSchema,
      resetAmazonState,
      fetchAmazonSchema,
    } = this.props.actions;

    if (!originalProduct) {
      clearAmazonErrors();
      resetAmazonState();
      fetchSchema('product');
      fetchProduct(productId);
    }

    if (categoryId) {
      fetchAmazonSchema(categoryId);
    }
  }

  // All product changes handles here through the local state
  componentWillUpdate(nextProps, nextState) {
    const { actions: { fetchAmazonSchema }, originalProduct } = this.props;
    const nextProduct = nextProps.originalProduct;

    const categoryId = this.nodeId();
    const nextCatId = this.nodeId(nextState.product);

    if (nextCatId && nextCatId != categoryId) {
      fetchAmazonSchema(nextCatId);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { originalProduct } = this.props;
    const nextProduct = nextProps.originalProduct;

    if (nextProduct && nextProduct !== originalProduct) {
      this.setState({ product: nextProduct });
    }
  }

  renderButtons() {
    const { saveBtnIsLoading, product } = this.state;
    const { originalProduct } = this.props;
    const productIsValid = this.validate();
    const disabled = !productIsValid || saveBtnIsLoading || product === originalProduct;

    return (
      <SaveCancel
        onCancel={this.handleCancel}
        saveText="Push to Amazon"
        saveDisabled={disabled}
        isLoading={saveBtnIsLoading}
      />
    );
  }

  renderVariants() {
    const { schema, fetchingSchema } = this.props;
    const { product } = this.state;

    if (!schema) {
      if (fetchingSchema) {
        return <div className={s.root}><WaitAnimation /></div>;
      }

      return null;
    }

    return (
      <ContentBox title="Variants Information">
        <ProductAmazonVariants
          product={product}
          onChange={this.handleProductChange}
        />
      </ContentBox>
    );
  }

  @autobind
  handleSubmit(e) {
    const { actions: { pushProduct, updateProduct } } = this.props;
    const { product } = this.state;

    this.setState({ saveBtnIsLoading: true });

    updateProduct(product)
      .then(() => pushProduct(product.id))
      .then(() => this.setState({ saveBtnIsLoading: false }))
      .catch((error) => this.setState({ error, saveBtnIsLoading: false }));
  }

  validate() {
    const { product } = this.state;
    const hasCategory = !!this.nodeId(product);
    const checkedVariants = product.skus.filter(sku => _.get(sku, 'attributes.amazon.v', false));
    const checkedVariantsHasInventory = checkedVariants.every(
      sku => _.get(sku, 'attributes.inventory.v', 0) > 0
    );
    const checkedVariantsHasUpc = checkedVariants.every(sku => !!_.get(sku, 'attributes.upc.v', ''));
    // @todo validate all other fields

    return hasCategory && checkedVariants.length && checkedVariantsHasInventory && checkedVariantsHasUpc;
  }

  @autobind
  handleCancel() {
    const { product } = this.state;

    transitionTo('product', {
      productId: product.id,
      context: 'default',
    });
  }

  @autobind
  handleProductChange(nextProduct) {
    this.setState({ product: nextProduct });
  }

  nodeId(product) {
    return _.get(product || this.state.product || this.props.originalProduct, 'attributes.nodeId.v', '');
  }

  render() {
    const { suggest, schema, productStatus, fetchingProduct } = this.props;
    const { product, error } = this.state;

    // @todo show errors/success notifications
    if (error) {
      console.error(error);
    }

    if (!product || fetchingProduct) {
      return <div className={s.root}><WaitAnimation /></div>;
    }

    // @todo productStatus

    return (
      <Form className={s.root} onSubmit={this.handleSubmit}>
        <header className={s.header}>
          <h1 className={s.title}>New Amazon Listing</h1>
          {this.renderButtons()}
        </header>
        <ContentBox title="Amazon Listing Information" className={s.box}>
          <ProductAmazonMain
            product={product}
            schema={schema}
            onChange={this.handleProductChange}
          />
        </ContentBox>
        {this.renderVariants()}
        <footer className={s.footer}>
          {this.renderButtons()}
        </footer>
      </Form>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
