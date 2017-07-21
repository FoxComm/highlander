/* @flow */

// libs
import React from 'react';
import { Link } from 'react-router';
import Icon from 'ui/icon';

// styles
import styles from './summary-line-item.css';

// components
import Currency from 'ui/currency';
import ProductImage from 'components/image/image';

type Props = {
  imagePath: string,
  name: string,
  quantity: number,
  price: number,
  totalPrice: number,
};

const LineItemRow = (props: Props) => {
  return (
    <div styleName="line-item">
      <div styleName="content">
        <div styleName="product-image">
          <ProductImage src={props.imagePath} width={50} height={50} />
        </div>
        <div styleName="product-data">
          <div styleName="product-info">
            <div styleName="product-name">{props.name}</div>
            <span styleName="gift-link">
              <Icon name="fc-gift" />&nbsp;
              <Link to="gift-cards">Make this a gift</Link>
            </span>
            <div styleName="product-variant">{/* TODO: variant info must be here */}</div>
          </div>
          <div styleName="price-data">
            <div styleName="price-and-quantity">
              <div styleName="qnt-block">{props.quantity}</div>
              <div>×</div>
              <div styleName="price-block"><Currency value={props.price} /></div>
            </div>
            <div styleName="product-price">
              <Currency value={props.totalPrice} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LineItemRow;
