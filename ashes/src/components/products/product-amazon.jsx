/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as productActions from 'modules/products/details';
import * as amazonActions from 'modules/products/amazon';
import * as schemaActions from 'modules/object-schema';
import s from './product-amazon.css';
import { Suggester } from 'components/suggester/suggester';

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
  const product = state.products.details.product || {};
  const suggest = state.products.amazon.suggest;

  return {
    title: product.attributes && product.attributes.title.v,
    suggest,
  };
}

type State = {
  categoryId: string,
}

class ProductAmazon extends Component {
  state: State = {
    categoryId: '',
  };

  componentDidMount() {
    const { productId } = this.props.params;
    const { clearFetchErrors, fetchSchema, fetchProduct } = this.props.actions;

    clearFetchErrors();
    fetchSchema('product');
    fetchProduct(productId);
  }

  render() {
    const { title, suggest } = this.props;

    return (
      <div className={s.root}>
        Amazon {title}
        <div className={s.suggesterWrapper}>
          <Suggester
            className={s.suggester}
            onChange={(text) => this._onTextChange(text)}
            onPick={(id) => this._onCatPick(id)}
            data={suggest} />
          <button className={s.set}>Set</button>
        </div>
      </div>
    );
  }

  _onTextChange(text) {
    const { params: { productId } } = this.props;

    this.props.actions.fetchSuggest(productId, text);
  }

  _onCatPick(categoryId) {
    this.setState({ categoryId });
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);
