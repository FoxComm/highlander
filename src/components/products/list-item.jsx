/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { browserHistory } from 'react-router';

type Product = {
  id: number;
  name: string;
  price: string;
  imageUrl: string;
}

const ListItem = (props: Product): HTMLElement => {
  const {id, name, price, imageUrl} = props;

  const click = () => browserHistory.push(`/products/${id}`);

  return (
    <div styleName="list-item" onClick={click}>
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
