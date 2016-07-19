/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { browserHistory } from 'react-router';
import _ from 'lodash';

import Currency from 'ui/currency';

type Image = {
  alt?: string,
  src: string,
  title?: string,
};

type Album = {
  name: string,
  images?: Array<Image>,
};

type Product = {
  id: number,
  productId: number,
  context: string,
  title: string,
  description: string,
  albums: Array<Album>,
  salePrice: string,
  currency: string,
  tags: Array<string>,
}

const ListItem = (props: Product): HTMLElement => {
  const {productId, title, albums, salePrice, currency} = props;

  const imageURL = _.get(albums, [0, 'images', 0, 'src']);
  const image = imageURL ? <img src={imageURL} styleName="preview-image" /> : null;

  const click = () => browserHistory.push(`/products/${productId}`);

  return (
    <div styleName="list-item" onClick={click}>
      <div styleName="preview">
        {image}
      </div>
      <div styleName="name">
        {title}
      </div>
      <div styleName="price">
        <Currency value={salePrice} currency={currency} />
      </div>
    </div>
  );
};

export default ListItem;
