/**
 * @flow weak
 * This component handles a product prepartion for Amazon
 * The product itself is in local state, and all modifications goes through it
 * The product passes by props to the descendant components, but modifies only in
 * immutable way though handlers
 * <Form> component is used as a wrapper to use its validation mechanism
 */

// libs
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// actions
import { transitionTo } from 'browserHistory';
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/channels/amazon';
import * as schemaActions from 'modules/object-schema';

// components
import Spinner from 'components/core/spinner';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import Form from 'components/forms/form';
import ProductAmazonMain from './product-amazon-main';
import ProductAmazonVariants from './product-amazon-variants';

// types
import type { AttrSchema } from 'paragons/object';

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
  const { schema, productStatus, credentials } = state.channels.amazon;

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

type Actions = {
  fetchSchema: Function,
  updateProduct: Function,
  fetchProduct: Function,
  clearAmazonErrors: Function,
  resetAmazonState: Function,
  fetchAmazonSchema: Function,
  pushProduct: Function,
  fetchProductStatus: Function,
};

type Params = {
  productId: string,
};

type Props = {
  originalProduct: ?Product,
  amazonEnabled: boolean,
  productStatus: any, // @todo
  fetchingProduct: boolean,
  fetchingSchema: boolean,
  schema: AttrSchema,
  actions: Actions,
  params: Params,
};

class ProductAmazon extends Component {
  state: State = {
    product: this.props.originalProduct,
    error: '',
    saveBtnIsLoading: false,
  };

  props: Props;

  componentDidMount() {
    const { productId } = this.props.params;
    const { originalProduct } = this.props;
    const categoryId = this.nodeId();
    const {
      clearAmazonErrors,
      fetchProduct,
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
    const { actions: { fetchAmazonSchema } } = this.props;

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
        saveLabel="Push to Amazon"
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
        return <div className={s.root}><Spinner /></div>;
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

    if (!product) {
      return;
    }

    this.setState({ saveBtnIsLoading: true });

    updateProduct(product)
      .then(() => pushProduct(product.id))
      .then(() => this.setState({ saveBtnIsLoading: false }))
      .catch((error) => this.setState({ error, saveBtnIsLoading: false }));
  }

  validate() {
    const { product } = this.state;

    if (!product) {
      return false;
    }

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
    const { productId } = this.props.params;

    transitionTo('product', {
      productId,
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
    const { schema, fetchingProduct } = this.props;
    const { product, error } = this.state;

    // @todo show errors/success notifications
    if (error) {
      console.error(error);
    }

    if (!product || fetchingProduct) {
      return <div className={s.root}><Spinner /></div>;
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
