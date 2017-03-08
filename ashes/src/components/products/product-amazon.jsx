/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import classNames from 'classnames';
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/channels/amazon';
import * as schemaActions from 'modules/object-schema';
import s from './product-amazon.css';
import { Suggester } from 'components/suggester/suggester';
import WaitAnimation from '../common/wait-animation';
import Progressbar from '../common/progressbar';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({
      ...amazonActions,
      ...productActions,
      ...schemaActions,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const product = state.products.details.product;
  const { suggest, schema } = state.channels.amazon;

  return {
    title: product && product.attributes && product.attributes.title.v,
    product,
    fetchingProduct: state.asyncActions.fetchProduct && state.asyncActions.fetchProduct.inProgress,
    suggest,
    schema,
  };
}

type State = {
  categoryId: string,
  stepNum: number,
  form: boolean,
};

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
    const { clearFetchErrors, fetchSchema, fetchProduct } = this.props.actions;

    if (!product) {
      clearFetchErrors();
      fetchSchema('product');
      fetchProduct(productId);
    }
  }

  render() {
    const { title, suggest, schema, product, fetchingProduct } = this.props;
    const { categoryId, stepNum } = this.state;
    const progressSteps = steps.map((step, i) => ({
      text: `${i+1}. ${step.text}`,
      current: i == stepNum,
      incompleted: i > stepNum,
    }));
    let productForm = [];

    if (schema) {
      const p = schema.properties.attributes.properties;

      for (let key in p) {
        const value = p[key];
        productForm.push(
          <div className="fc-form-field fc-object-form__field">
            <div className="fc-form-field-label">{key}</div>
            <input type="text" className="fc-object-form__field-value" />
          </div>
        );
      }
    }

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
          />
        </div>

        {this.state.form && productForm}
      </div>
    );
  }

  _onTextChange(text) {
    const { params: { productId } } = this.props;

    this.props.actions.fetchSuggest(productId, text);
  }

  _onCatPick(categoryId) {
    this.setState({ categoryId });
    this._setCat(categoryId);
  }

  _setCat(categoryId) {
    const { fetchAmazonSchema } = this.props.actions;

    this.setState({
      form: true,
      stepNum: 1,
    });

    fetchAmazonSchema();

    // @todo call action to fetch schema
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
