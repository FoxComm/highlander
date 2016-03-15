/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';

type Product = {
  id: number;
  name: string;
  price: string;
  imageUrl: string;
}

const ListItem = (props: Product): HTMLElement => {
  const {name, price, imageUrl} = props;
  return (
    <div styleName="list-item">
      <div styleName="preview">
        <img src={imageUrl} styleName="preview-image" />
      </div>
      <div styleName="name">
        {name}
      </div>
      <div styleName="price">
        {price}
      </div>
    </div>
  );
};

export default ListItem;
