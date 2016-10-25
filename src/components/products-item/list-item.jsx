/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { browserHistory } from 'react-router';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { addLineItem } from 'modules/cart';
import { connect } from 'react-redux';
import cx from 'classnames';

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

const mapStateToProps = (state, props) => {
  const lineItems = _.get(state, ['cart', 'skus'], []);
  const itemAddedToCart = _.find(lineItems, item => item.sku === props.skuId);

  return {
    itemAddedToCart,
  };
};

class ListItem extends React.Component {
  props: Product;

  @autobind
  addToCart () {
    if (this.props.itemAddedToCart) {
      return;
    }

    const quantity = 1;

    this.props.addLineItem(this.props.skuId, quantity)
      .then(() => {

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

    const click = () => browserHistory.push(`/products/${productId}`);
    const btnCls = cx(styles['add-to-cart-btn'], {
      [styles['item-added']]: this.props.itemAddedToCart,
    });

    return (
      <div styleName="list-item">
        {previewImageUrl &&
          <div styleName="preview" onClick={click}>
            <img src={previewImageUrl} styleName="preview-image" />
            <div styleName="hover-info">
              <h2 styleName="additional-description">{description}</h2>
            </div>
          </div>}

        <div styleName="text-block">
          <h1 styleName="title" alt={title}>
            {title}
          </h1>
          <h2 styleName="description">{description}</h2>
          <div styleName="price-line">
            <div styleName="price">
              <Currency value={salePrice} currency={currency} />
            </div>

            <div styleName="add-to-cart" onClick={this.addToCart}>
              <button className={btnCls}>
                <span styleName="add-icon">{this.props.itemAddedToCart ? '✓' : '+'}</span>
              </button>

              {!this.props.itemAddedToCart &&
                <div styleName="add-title-expanded">ADD TO CART</div>}
            </div>

          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, {
  addLineItem,
})(ListItem);
