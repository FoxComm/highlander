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
import Form from 'components/forms/form';
import ProductAmazonMain from './product-amazon-main';
import ProductAmazonVariants from './product-amazon-variants';

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
    pushingProduct: _.get(state.asyncActions, 'pushToAmazon.inProgress'),
    schema,
  };
}

type State = {
  product: Object,
  error: any,
};

class ProductAmazon extends Component {
  state: State = {
    product: this.props.originalProduct,
    error: '',
  };

  componentDidMount() {
    const { productId } = this.props.params;
    const { originalProduct } = this.props;
    const categoryId = this._nodeId();
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

    const categoryId = this._nodeId();
    const nextCatId = this._nodeId(nextState.product);

    if (nextCatId && nextCatId != categoryId) {
      fetchAmazonSchema(nextCatId);
    }

    if (nextProduct && (originalProduct && nextProduct.id != originalProduct.id || !originalProduct)) {
      this.setState({ product: nextProduct })
    }
  }

  renderButtons() {
    const productIsValid = this._validate();

    return [
      <Button onClick={this._handleCancel} key="cancel">Cancel</Button>,
      <PrimaryButton
        className={s.saveBtn}
        disabled={!productIsValid}
        key="push"
        type="submit"
      >
        Push to Amazon
      </PrimaryButton>
    ];
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
          onChange={this._handleProductChange}
        />
      </ContentBox>
    );
  }

  render() {
    const { suggest, schema, productStatus, fetchingProduct } = this.props;
    const { product, error } = this.state;

    if (error) {
      console.error(error);
    }

    if (!product || fetchingProduct) {
      return <div className={s.root}><WaitAnimation /></div>;
    }

    // @todo productStatus

    return (
      <Form className={s.root} onSubmit={() => this._handleSubmit()}>
        <header className={s.header}>
          <h1 className={s.title}>New Amazon Listing</h1>
          {this.renderButtons()}
        </header>
        <ContentBox title="Amazon Listing Information" className={s.box}>
          <ProductAmazonMain
            product={product}
            schema={schema}
            onChange={this._handleProductChange}
          />
        </ContentBox>
        {this.renderVariants()}
        <footer className={s.footer}>
          {this.renderButtons()}
        </footer>
      </Form>
    );
  }

  _handleSubmit(e) {
    this._handlePush();
  }

  _validate() {
    const { product } = this.state;
    const hasCategory = !!this._nodeId(product);
    const checkedVariants = product.skus.filter(sku => _.get(sku, 'attributes.amazon.v', false));
    const checkedVariantsHasInventory = checkedVariants.every(
      sku => _.get(sku, 'attributes.inventory.v', 0) > 0
    );
    const checkedVariantsHasUpc = checkedVariants.every(sku => !!_.get(sku, 'attributes.upc.v', ''));
    // @todo validate all other fields

    return hasCategory && checkedVariants.length && checkedVariantsHasInventory && checkedVariantsHasUpc;
  }

  @autobind
  _handleCancel() {
    const { product } = this.state;

    transitionTo('product', {
      productId: product.id,
      context: 'default',
    });
  }

  @autobind
  _handleProductChange(nextProduct) {
    const { actions: { updateProduct } } = this.props;

    this.setState({ product: nextProduct });
  }

  @autobind
  _handlePush() {
    const { actions: { pushProduct, updateProduct } } = this.props;
    const { product } = this.state;

    updateProduct(product)
      // .then(() => pushProduct(product.id))
      .catch((error) => this.setState({ error }));
  }

  _nodeId(product = this.state.product) {
    return _.get(product, 'attributes.nodeId.v', '');
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
