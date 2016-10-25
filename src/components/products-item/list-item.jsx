/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { Link } from 'react-router';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { addLineItem, toggleCart } from 'modules/cart';
import { connect } from 'react-redux';

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
  skus: Array<string>,
  addLineItem: Function,
  toggleCart: Function,
};

class ListItem extends React.Component {
  props: Product;

  static defaultProps = {
    skus: [],
  }

  @autobind
  addOrRemoveFromCart () {
    const skuId = this.props.skus[0];
    const quantity = 1;

    this.props.addLineItem(skuId, quantity)
      .then(() => {
        this.props.toggleCart();
      })
      .catch(ex => {
        this.setState({
          error: ex,
        });
      });
  }

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

    return (
      <div styleName="list-item">
        {previewImageUrl &&
          <Link to={`/products/${productId}`}>
            <div styleName="preview">
              <img src={previewImageUrl} styleName="preview-image" />
              <div styleName="hover-info">
                <h2 styleName="additional-description">{description}</h2>
              </div>
            </div>
          </Link>
          }

        <div styleName="text-block">
          <h1 styleName="title" alt={title}>
            {title}
          </h1>
          <h2 styleName="description">{/* serving size */}</h2>
          <div styleName="price-line">
            <div styleName="price">
              <Currency value={salePrice} currency={currency} />
            </div>

            <div styleName="add-to-cart" onClick={this.addOrRemoveFromCart}>
              <button styleName="add-to-cart-btn">
                <span styleName="add-icon">+</span>
              </button>
              <div styleName="add-title-expanded">ADD TO CART</div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(null, {
  addLineItem,
  toggleCart,
})(ListItem);
