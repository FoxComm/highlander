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
  location: any;
}

const mapStateToProps = state => ({categories: state.categories.list});

class ProductsList extends React.Component {
  props: ProductsListParams;

  renderHeader() {
    const props = this.props;

    if (!props.category) return;

    const category = _.find(props.categories, {name: props.category});
    const description = (category && category.description) ? <p styleName="description">{category.description}</p> : '';

    let className = `header-${props.category}`;
    let title = props.category;

    if (props.location.query && props.location.query.type) {
      className = `${className}-${props.location.query.type}`;
      title = `${props.location.query.type}'s ${title}`;
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

  render() : HTMLElement {
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
}

export default connect(mapStateToProps, {})(ProductsList);
