/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { Link } from 'react-router';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { addLineItem, deleteLineItem } from 'modules/cart';
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
  itemAddedToCart: boolean,
  skus: Array<String>,
  addLineItem: Function,
  deleteLineItem: Function,
};

const mapStateToProps = (state, props) => {
  const lineItems = _.get(state, ['cart', 'skus'], []);
  const skuId = props.skus[0];
  const itemAddedToCart = !!_.find(lineItems, item => item.sku === skuId);

  return {
    itemAddedToCart,
  };
};

class ListItem extends React.Component {
  props: Product;

  static defaultProps = {
    skus: [],
  }

  @autobind
  addOrRemoveFromCart () {
    const skuId = this.props.skus[0];

    if (this.props.itemAddedToCart) {
      this.props.deleteLineItem(skuId).catch(ex => {
        this.setState({
          error: ex,
        });
      });
    } else {
      const quantity = 1;

      this.props.addLineItem(skuId, quantity)
        .then(() => {

        })
        .catch(ex => {
          this.setState({
            error: ex,
          });
        });
    }
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

    const btnCls = cx(styles['add-to-cart-btn'], {
      [styles['item-added']]: this.props.itemAddedToCart,
    });

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
          <h2 styleName="description">{description}</h2>
          <div styleName="price-line">
            <div styleName="price">
              <Currency value={salePrice} currency={currency} />
            </div>

            <div styleName="add-to-cart" onClick={this.addOrRemoveFromCart}>
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
  deleteLineItem,
})(ListItem);
