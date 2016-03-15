/* @flow */

import React, { Component } from 'react';
import _ from 'lodash';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import styles from './product-list.css';
import { connect } from 'react-redux';

import ListItem from '../../products/list-item';

import * as actions from 'modules/products';

type Params = {
  categoryName: ?string;
}

type ProductListParams = {
  params: Params;
  list: Array<Product>;
  fetch: Function;
}

const getState = state => ({...state.products});

class ProductList extends Component {

  componentWillMount() {
    const categoryId = this.categoryId(this.props.params);
    this.props.fetch(categoryId);
  }

  componentWillReceiveProps(nextProps: ProductListParams) {
    const categoryId = this.categoryId(this.props.params);
    const nextId = this.categoryId(nextProps.params);
    if (categoryId !== nextId) {
      this.props.fetch(nextId);
    }
  }

  categoryId(params: Params): ?number {
    const id = params.categoryName ? parseInt(params.categoryName, 10) : null;
    return isNaN(id) ? null : id;
  }

  render(): HTMLElement {
    const items = _.map(this.props.list, (item) => <ListItem {...item} key={`product-${item.id}`} />);
    return (
      <div styleName="catalog">
        {items}
      </div>
    );
  }
}

export default connect(getState, actions)(ProductList);
