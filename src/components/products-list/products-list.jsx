/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import { connect } from 'react-redux';

import styles from './products-list.css';

import ListItem from '../products-item/list-item';

type Category = {
  name: string;
  id: number;
  description: string;
};

type ProductsListParams = {
  list: ?Array<Product>;
  categories: ?Array<Category>;
  category: ?string;
}

const mapStateToProps = state => ({
  categories: state.categories.list
});

class ProductsList extends React.Component {

  renderHeader() {
    const props = this.props;
    const category = props.category
      ? _.find(props.categories, {name: props.category})
      : '';

    if (category && category.description) {
      return (
        <header styleName="header">
          <h1>{props.category}</h1>
          <p>{category.description}</p>

        </header>
      )
    };
  }

  render() {
    const props = this.props;
    const items = props.list && props.list.length > 0
      ? _.map(props.list, (item) => <ListItem {...item} key={`product-${item.id}`}/>)
      : <div styleName="not-found">No products found.</div>;

    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="list">
          {items}
        </div>
      </section>
    );
  }
};

export default connect(mapStateToProps, {})(ProductsList);