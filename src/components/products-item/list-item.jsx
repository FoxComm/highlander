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
  images: Array<Image>,
};

type Product = {
  id: number,
  productId: number,
  context: string,
  title: string,
  description: string,
  salePrice: string,
  currency: string,
  albums: ?Array<Album>,
};

class ListItem extends React.Component {
  props: Product;

  render(): HTMLElement {
    const {
      productId,
      title,
      description,
      albums,
      salePrice,
      currency,
    } = this.props;

    const previewImageUrl = _.get(albums, [0, 'images', 0, 'src']);

    const click = () => browserHistory.push(`/products/${productId}`);

    return (
      <div styleName="list-item">
        {previewImageUrl &&
          <div styleName="preview" onClick={click}>
            <img src={previewImageUrl} styleName="preview-image" />
            {false && <div styleName="hover-info"></div>}
          </div>}

        <div styleName="bottom-stack">
          <div styleName="title">
            {title}
          </div>
          <div styleName="divider"></div>
          <div styleName="description">{description}</div>
          <div styleName="price-line">
            <div styleName="price">
              <Currency value={salePrice} currency={currency} />
            </div>
            <div styleName="add-to-cart">
              <div styleName="add-to-cart-btn">
                <div styleName="add-icon">
                  <span>+</span>
                </div>
              </div>
              <div styleName="add-title-expanded">ADD TO CART</div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default ListItem;
