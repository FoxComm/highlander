/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import styles from './product.css';
import { connect } from 'react-redux';

import ProductsList from '../../products-list/products-list';

import * as actions from 'modules/products';

type Params = {
  categoryName: ?string;
}

type ProductListParams = {
  params: Params;
  list: Array<Product>;
  isLoading: boolean;
  fetch: Function;
}

const getState = state => {
  const async = state.asyncActions.products;

  return {
    ...state.products,
    isLoading: !!async ? async.inProgress : true,
  };
};

class Products extends Component {

  componentWillMount() {
    const { categoryName } = this.props.params;
    this.props.fetch(categoryName);
  }

  componentWillReceiveProps(nextProps: ProductListParams) {
    const { categoryName } = this.props.params;
    this.props.fetch(categoryName);
  }

  categoryId(params: Params): ?number {
    const id = params.categoryName ? parseInt(params.categoryName, 10) : null;
    return isNaN(id) ? null : id;
  }

  render(): HTMLElement {
    return <ProductsList list={this.props.list}/>;
  }
}

export default connect(getState, actions)(Products);
