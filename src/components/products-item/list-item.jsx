/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import styles from './list-item.css';
import { Link } from 'react-router';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { addLineItem, toggleCart } from 'modules/cart';
import { connect } from 'react-redux';

import AddToCartBtn from 'ui/add-to-cart-btn';
import Currency from 'ui/currency';
import ImagePlaceholder from './image-placeholder';

type Image = {
  alt?: string,
  src: string,
  title?: string,
  baseurl?: string,
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
  description: ?string,
  salePrice: string,
  currency: string,
  albums: ?Array<Album> | Object,
  skus: Array<string>,
  addLineItem: Function,
  toggleCart: Function,
};

type State = {
  error?: any,
};

class ListItem extends React.Component {
  props: Product;
  state: State;

  static defaultProps = {
    skus: [],
  };

  @autobind
  addToCart () {
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

  get image() {
    const previewImageUrl = _.get(this.props.albums, [0, 'images', 0, 'src']);

    return previewImageUrl
      ? <img src={previewImageUrl} styleName="preview-image"/>
      : <ImagePlaceholder/>;
  }

  render(): HTMLElement {
    const {
      productId,
      title,
      description,
      salePrice,
      currency,
    } = this.props;

    return (
      <div styleName="list-item">
        <Link to={`/products/${productId}`}>
          <div styleName="preview">
            {this.image}
            <div styleName="hover-info">
              <h2
                styleName="additional-description"
                dangerouslySetInnerHTML={{__html: description}}
              />
            </div>
          </div>
        </Link>

        <div styleName="text-block">
          <h1 styleName="title" alt={title}>
            <Link to={`/products/${productId}`}>{title}</Link>
          </h1>
          <h2 styleName="description">{/* serving size */}</h2>
          <div styleName="price-line">
            <div styleName="price">
              <Currency value={salePrice} currency={currency} />
            </div>

            <AddToCartBtn onClick={this.addToCart} expanded />
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
