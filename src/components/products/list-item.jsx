/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import cssModules from 'react-css-modules';
import styles from './list-item.css';

// type Product {
//   id: number;
//   attributes: Object;
// };

const ListItem = (): HTMLElement => {
  const price = '$50';
  const name = 'lorem ipsum';
  const imageUrl = 'http://lorempixel.com/75/75/fashion/';
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

export default cssModules(ListItem, styles);
