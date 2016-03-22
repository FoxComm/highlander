/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import styles from './products-list.css';

import ListItem from '../products-item/list-item';

type ProductsListParams = {
  list: Array<Product>;
}

const ProductsList = (props:ProductsListParams):HTMLElement => {
  const items = _.map(props.list, (item) => <ListItem {...item} key={`product-${item.id}`}/>);

  return (
    <div styleName="catalog">
      {items}
    </div>
  );
};

export default ProductsList;
