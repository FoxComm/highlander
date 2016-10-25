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
  categoryName: ?string,
  productType: ?string
};

type Props = {
  params: Params,
  list: Array<Product>,
  isLoading: boolean,
  fetch: Function,
  location: any,
};

const mapStateToProps = state => {
  const async = state.asyncActions.products;

  return {
    ...state.products,
    isLoading: !!async ? async.inProgress : true,
  };
};

class Products extends Component {
  props: Props;

  componentWillMount() {
    const { categoryName, productType } = this.props.params;
    this.props.fetch(categoryName, productType);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { categoryName, productType } = this.props.params;
    const {
      categoryName: nextCategoryName,
      productType: nextProductType,
    } = nextProps.params;

    if ((categoryName !== nextCategoryName) || (productType !== nextProductType)) {
      this.props.fetch(nextCategoryName, nextProductType);
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
        categoryName={this.props.params.categoryName}
        productType={this.props.params.productType}
      />;
  }
}

export default connect(mapStateToProps, actions)(Products);
