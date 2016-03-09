/* @flow */

import React from 'react';
import _ from 'lodash';
import type { HTMLElement } from 'types';
import cssModules from 'react-css-modules';
import styles from './product-list.css';

import ListItem from '../../products/list-item';

// type ProductListParams = {
//   params: Object;
// }

const mockedData = [
  {
    id: 1,
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    id: 2,
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    id: 3,
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    id: 4,
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    id: 5,
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    id: 6,
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
];

const ProductList = (): HTMLElement => {
  const items = _.map(mockedData, (item) => <ListItem {...item} key={`product-${item.id}`} />);
  return (
    <div styleName="catalog">
      {items}
    </div>
  );
};

export default cssModules(ProductList, styles);
