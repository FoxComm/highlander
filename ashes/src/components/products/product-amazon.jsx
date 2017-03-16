/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import classNames from 'classnames';
import _ from 'lodash';
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/channels/amazon';
import * as schemaActions from 'modules/object-schema';
import s from './product-amazon.css';
import { Suggester } from 'components/suggester/suggester';
import WaitAnimation from '../common/wait-animation';
import Progressbar from '../common/progressbar';
import { getSuggest } from './selector';
import ObjectFormInner from '../object-form/object-form-inner';
import ProductAmazonForm from './product-amazon-form';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({
      ...schemaActions,
      updateProduct: productActions.updateProduct,
      fetchProduct: productActions.fetchProduct,
      clearAmazonErrors: amazonActions.clearErrors,
      fetchAmazonSchema: amazonActions.fetchAmazonSchema,
      fetchSuggest: amazonActions.fetchSuggest,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const product = state.products.details.product;
  const { suggest, schema } = state.channels.amazon;

  return {
    title: product && product.attributes && product.attributes.title.v,
    product,
    fetchingProduct: _.get(state.asyncActions, 'fetchProduct.inProgress'),
    fetchingSuggest: _.get(state.asyncActions, 'fetchSuggest.inProgress'),
    fetchingSchema: _.get(state.asyncActions, 'fetchAmazonSchema.inProgress'),
    suggest: getSuggest(suggest),
    schema,
  };
}

type State = {
  categoryId: string,
  stepNum: number,
  form: boolean,
};

// @todo maybe move to another component?
const steps = [{
  text: 'Choose Category'
}, {
  text: 'Fill all fields'
}, {
  text: 'Submit'
}];

class ProductAmazon extends Component {
  state: State = {
    categoryId: '',
    stepNum: 0,
    form: false,
  };

  componentDidMount() {
    const { productId } = this.props.params;
    const { product } = this.props;
    const { clearAmazonErrors, fetchSchema, fetchProduct } = this.props.actions;

    if (!product) {
      clearAmazonErrors();
      fetchSchema('product');
      fetchProduct(productId);
    }
  }

  // @todo move to the new component
  renderForm() {
    const { schema, product, fetchingSchema } = this.props;
    const { categoryId } = this.state;

    if (!schema) {
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
        onSubmit={(p) => this._handleSubmit(p)} />
    );
  }

  _handleChange(e, name) {
    this.setState({
      [name]: e.target.value
    });
  }

  _handleSubmit(nextProduct) {
    const { actions: { updateProduct } } = this.props;

    updateProduct(nextProduct);
  }

  render() {
    const { title, suggest, product, fetchingProduct, fetchingSuggest } = this.props;
    const { categoryId, stepNum } = this.state;
    const progressSteps = steps.map((step, i) => ({
      text: `${i+1}. ${step.text}`,
      current: i == stepNum,
      incompleted: i > stepNum,
    }));

    if (!product || fetchingProduct) {
      return <div className={s.root}><WaitAnimation /></div>;
    }

    return (
      <div className={s.root}>
        <Progressbar steps={progressSteps} className={s.progressbar} />
        <h1>{title} for Amazon</h1>
        <h2>Choose Amazon category:</h2>
        <div className={s.suggesterWrapper}>
          <Suggester
            className={s.suggester}
            onChange={(text) => this._onTextChange(text)}
            onPick={(id) => this._onCatPick(id)}
            data={suggest}
            inProgress={fetchingSuggest}
          />
        </div>

        {this.renderForm()}
      </div>
    );
  }

  _onTextChange(text) {
    const { title } = this.props;

    this.props.actions.fetchSuggest(title, text);
  }

  _onCatPick(categoryId) {
    this.setState({ categoryId });
    this._setCat(categoryId);
  }

  _setCat(categoryId) {
    const { fetchAmazonSchema } = this.props.actions;

    this.setState({
      stepNum: 1,
    });

    fetchAmazonSchema();
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
