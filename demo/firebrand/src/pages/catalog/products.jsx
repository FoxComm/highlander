/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import { connect } from 'react-redux';

import Loader from 'ui/loader';
import ProductsList from '../../components/products-list/products-list';

import styles from './products.css';

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

const mapStateToProps = state => {
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
    const nextName = nextProps.params.categoryName;
    if (categoryName !== nextName) {
      this.props.fetch(nextName);
    }
  }

  categoryId(params: Params): ?number {
    const id = params.categoryName ? parseInt(params.categoryName, 10) : null;
    return isNaN(id) ? null : id;
  }

  render(): HTMLElement {
    return this.props.isLoading
      ? <Loader/>
      : <ProductsList
        list={this.props.list}
        category={this.props.params.categoryName}
        categoryType={this.props.location.query.type}
      />;
  }
}

export default connect(mapStateToProps, actions)(Products);
