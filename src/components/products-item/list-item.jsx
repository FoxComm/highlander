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
  images: ?Array<string>,
  salePrice: string,
  currency: string,
};

class ListItem extends React.Component {
  props: Product;

  render() {
    const {productId, title, images, salePrice, currency} = this.props;

    const image = images && images.length > 0
      ? <img src={images[0]} styleName="preview-image" />
      : null;

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
          <Currency value={salePrice} currency={currency}/>
        </div>
      </div>
    );
  }
};

export default ListItem;
