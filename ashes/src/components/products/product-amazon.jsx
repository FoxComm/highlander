/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import classNames from 'classnames';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// actions
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/channels/amazon';
import * as schemaActions from 'modules/object-schema';

// components
import WaitAnimation from 'components/common/wait-animation';
import ObjectFormInner from 'components/object-form/object-form-inner';
import ProductAmazonForm from './product-amazon-form';
import ContentBox from 'components/content-box/content-box';
import { PrimaryButton } from 'components/common/buttons';
import Typeahead from 'components/typeahead/typeahead';
import { CategoryItem } from './category-item';

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
      fetchSuggest: amazonActions.fetchSuggest,
      pushProduct: amazonActions.pushToAmazon,
      fetchProductStatus: amazonActions.fetchAmazonProductStatus,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const product = state.products.details.product;
  const { suggest, schema, productStatus, credentials } = state.channels.amazon;

  return {
    title: product && product.attributes && product.attributes.title.v,
    product,
    amazonEnabled: !!credentials,
    productStatus,
    fetchingProduct: _.get(state.asyncActions, 'fetchProduct.inProgress'),
    fetchingSuggest: _.get(state.asyncActions, 'fetchSuggest.inProgress'),
    fetchingSchema: _.get(state.asyncActions, 'fetchAmazonSchema.inProgress'),
    pushingProduct: _.get(state.asyncActions, 'pushToAmazon.inProgress'),
    suggest: getSuggest(suggest),
    schema,
  };
}

type State = {
  categoryId: string,
  categoryPath: string,
};

class ProductAmazon extends Component {
  state: State = {
    categoryId: '',
    categoryPath: '',
  };

  componentDidMount() {
    const { productId } = this.props.params;
    const { product } = this.props;
    const { clearAmazonErrors, fetchProduct, fetchProductStatus, fetchSchema, resetAmazonState } = this.props.actions;

    if (!product) {
      clearAmazonErrors();
      resetAmazonState();
      fetchSchema('product');
      fetchProduct(productId);
    }
  }

  componentWillUpdate(nextProps) {
    const nodeId = _.get(this.props.product, ['attributes', 'nodeId', 'v'], null);
    const nextNodeId = _.get(nextProps.product, ['attributes', 'nodeId', 'v'], null);
    const nextNodePath = _.get(nextProps.product, ['attributes', 'nodePath', 'v'], null);

    if (nextNodeId && nextNodeId != nodeId && nextNodePath) {
      this._setCat(nextNodeId, nextNodePath);
    }
  }

  renderForm() {
    const { schema, product, fetchingSchema } = this.props;
    const { categoryId, categoryPath } = this.state;

    if (!schema || !product) {
      if (fetchingSchema) {
        return <WaitAnimation />;
      }

      return null;
    }

    return (
      <ProductAmazonForm
        schema={schema}
        product={product}
        categoryId={categoryId}
        categoryPath={categoryPath}
        onSubmit={this._handleSubmit}
      />
    );
  }

  render() {
    const { title, suggest, product, productStatus, fetchingProduct, fetchingSuggest } = this.props;

    if (!product || fetchingProduct) {
      return <div className={s.root}><WaitAnimation /></div>;
    }

    // @todo productStatus

    return (
      <div className={s.root}>
        <h1>{title} for Amazon</h1>
        <ContentBox title="Amazon Listing Information">
          <div className={s.suggesterWrapper}>
            <Typeahead
              className={s.suggester}
              onItemSelected={this._onCatPick}
              items={suggest}
              isFetching={fetchingSuggest}
              fetchItems={this._handleFetch}
              component={CategoryItem}
              initialValue={this.state.categoryPath}
            />
          </div>
        </ContentBox>
        {this.renderForm()}
        <PrimaryButton onClick={this._handlePush}>Push</PrimaryButton>
      </div>
    );
  }

  _getNodeId() {
    const { product } = this.props;

    return _.get(product, ['attributes', 'nodeId', 'v'], null);
  }

  @autobind
  _handleFetch(text) {
    const { title } = this.props;

    this.props.actions.fetchSuggest(title, text);
  }

  @autobind
  _onCatPick(item) {
    const { id, path } = item;

    this._setCat(id, path);
  }

  _setCat(id, path) {
    const { fetchAmazonSchema } = this.props.actions;

    this.setState({ categoryId: id, categoryPath: cat(path) });

    fetchAmazonSchema(id);
  }

  @autobind
  _handleSubmit(nextProduct) {
    const { actions: { updateProduct } } = this.props;

    updateProduct(nextProduct);
  }

  @autobind
  _handlePush() {
    const { product, actions: { pushProduct } } = this.props;

    pushProduct(product.id);
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
