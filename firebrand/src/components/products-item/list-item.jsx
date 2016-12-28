/* @flow */

// libs
import React from 'react';
import _ from 'lodash';
import { browserHistory } from 'react-router';

// components
import Currency from 'ui/currency';

// types
import type { HTMLElement } from 'types';

// modules
import { trackRequest } from 'modules/tracking';

// styles
import styles from './list-item.css';

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

  componentDidMount() {
    const { productId } = this.props;
    trackRequest({
      url: "/api/v1/hal",
      channel: 1,
      subject: 1,
      verb: 'list',
      obj: 'product',
      objId: productId
    });
  }

  render(): HTMLElement {
    const {productId, title, albums, salePrice, currency} = this.props;
    const previewImage = _.get(albums, [0, 'images', 0, 'src']);

    const image = previewImage
      ? <img src={previewImage} styleName="preview-image" />
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
}

export default ListItem;
