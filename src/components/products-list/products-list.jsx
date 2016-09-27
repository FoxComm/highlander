/* @flow */

import _ from 'lodash';
import React from 'react';
import { findDOMNode } from 'react-dom';
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
  categoryType: ?string;
}

const mapStateToProps = state => ({categories: state.categories.list});

class ProductsList extends React.Component {
  props: ProductsListParams;

  renderHeader() {
    const props = this.props;
    const categoryName = props.category;

    if (!categoryName) return;

    const categoryInfo = _.find(props.categories, {name: categoryName});
    const description = (categoryInfo && categoryInfo.description)
      ? <p styleName="description">{categoryInfo.description}</p>
      : '';

    let className = `header-${categoryName}`;
    let title = categoryName;

    if (props.categoryType) {
      className = `${className}-${props.categoryType}`;
      title = `${props.categoryType}'s ${title}`;
    }

    return (
      <header styleName={className}>
        <div styleName="header-wrap">
          <h1 styleName="title">{title}</h1>
          {description}
        </div>
      </header>
    );
  }

  getItemList() {
    return _.map(this.props.list, (item) => {
      return (
        <ListItem {...item} key={`product-${item.id}`} ref={`product-${item.id}`}/>
      );
    });
  }

  render() : HTMLElement {
    const props = this.props;
    const items = props.list && props.list.length > 0
      ? this.getItemList()
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
}

export default connect(mapStateToProps, {})(ProductsList);
