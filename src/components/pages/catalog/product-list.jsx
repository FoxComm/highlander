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
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
  {
    name: 'Donkey',
    imageUrl: 'http://lorempixel.com/75/75/fashion/',
    price: '50$',
  },
];

const ProductList = (): HTMLElement => {
  const items = _.map(mockedData, (item) => <ListItem {...item} />);
  return (
    <div styleName="catalog">
      {items}
    </div>
  );
};

export default cssModules(ProductList, styles);
