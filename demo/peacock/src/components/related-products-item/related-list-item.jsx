/* @flow */

import React, { Element } from 'react';
import { findDOMNode } from 'react-dom';
import type { HTMLElement } from 'types';
import styles from './related-list-item.css';
import { Link } from 'react-router';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import * as tracking from 'lib/analytics';

import Currency from 'ui/currency';
import ImagePlaceholder from '../products-item/image-placeholder';

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
  index: number,
  productId: number,
  slug: ?string,
  context: string,
  title: string,
  description: ?string,
  salePrice: string,
  retailPrice: string,
  currency: string,
  albums: ?Array<Album> | Object,
  tags?: Array<string>,
  addLineItem?: ?Function,
  toggleCart?: ?Function,
};

type State = {
  error?: any,
};

class RelatedListItem extends React.Component {
  props: Product;
  state: State;

  get image(): Element<any> {
    const previewImageUrl = _.get(this.props.albums, [0, 'images', 0, 'src']);

    return previewImageUrl
      ? <img src={previewImageUrl} styleName="preview-image" ref="image" />
      : <ImagePlaceholder ref="image" />;
  }

  getImageNode() {
    return findDOMNode(this.refs.image);
  }

  @autobind
  handleClick() {
    const { props } = this;

    tracking.clickPdp(props, props.index);
  }

  isOnSale(): HTMLElement {
    const { currency } = this.props;

    let {
      salePrice,
      retailPrice,
    } = this.props;

    salePrice = Number(salePrice);
    retailPrice = Number(retailPrice);

    return (retailPrice > salePrice) ? (
      <div styleName="price">
        <Currency
          styleName="retail-price"
          value={retailPrice}
          currency={currency}
        />
        <Currency
          styleName="on-sale-price"
          value={salePrice}
          currency={currency}
        />
      </div>
      ) : (
        <div styleName="price">
          <Currency value={salePrice} currency={currency} />
        </div>
      );
  }

  render(): HTMLElement {
    const {
      productId,
      slug,
      title,
    } = this.props;

    const productSlug = slug != null && !_.isEmpty(slug) ? slug : productId;

    return (
      <div styleName="list-item">
        <Link onClick={this.handleClick} to={`/products/${productSlug}`}>
          <div styleName="preview">
            {this.image}
          </div>
        </Link>

        <div styleName="text-block">
          <h1 styleName="title" alt={title}>
            <Link to={`/products/${productSlug}`}>{title}</Link>
          </h1>
          <div styleName="price-line">
            {this.isOnSale()}
          </div>
        </div>
      </div>
    );
  }
}

export default RelatedListItem;
