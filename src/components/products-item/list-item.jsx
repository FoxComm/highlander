/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { browserHistory } from 'react-router';

import Currency from 'ui/currency';

type Product = {
  id: number;
  context: string,
  title: string;
  description: string,
  images: Array<string>,
}

const ListItem = (props: Product): HTMLElement => {
  const {id, title, images} = props;
  const imageUrl = images[0];
  const price = 9999;

  const click = () => browserHistory.push(`/products/${id}`);

  return (
    <div styleName="list-item" onClick={click}>
      <div styleName="preview">
        <img src={imageUrl} styleName="preview-image" />
      </div>
      <div styleName="name">
        {title}
      </div>
      <div styleName="price">
        <Currency value={price} />
      </div>
    </div>
  );
};

export default ListItem;
