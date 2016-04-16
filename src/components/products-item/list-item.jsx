/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { browserHistory } from 'react-router';

import Currency from 'ui/currency';

type Product = {
  id: number,
  productId: number,
  context: string,
  title: string,
  description: string,
  images: Array<string>,
  salePrice: string,
  currency: string,
}

const ListItem = (props: Product): HTMLElement => {
  const {productId, title, images, salePrice, currency} = props;

  const imageUrl = images[0];

  const click = () => browserHistory.push(`/products/${productId}`);

  return (
    <div styleName="list-item" onClick={click}>
      <div styleName="preview">
        <img src={imageUrl} styleName="preview-image" />
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
