/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import styles from './products-list.css';

import ListItem from '../products-item/list-item';

type ProductsListParams = {
  list: ?Array<Product>;
}

const ProductsList = (props: ProductsListParams):HTMLElement => {
  const items = props.list && props.list.length > 0
    ? _.map(props.list, (item) => <ListItem {...item} key={`product-${item.id}`}/>)
    : <div styleName="not-found">No products found.</div>;

  return (
    <section styleName="catalog">
      <header>
        <h1></h1>
      </header>
      {items}
    </section>
  );
};

export default ProductsList;
