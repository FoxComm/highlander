/* @flow */

// libs
import React from 'react';

// styles
import styles from './summary-line-item.css';

// components
import Currency from 'ui/currency';

type Props = {
  imagePath: string,
  name: string,
  quantity: number,
  totalPrice: number,
};

const LineItemRow = (props: Props) => {
  console.log(props);
  return (
    <div styleName="line-item">
      <div styleName="content">
      <div styleName="product-image">
        <img src={props.imagePath} />
      </div>
      <div styleName="product-info">
        <div styleName="product-name">{props.name}</div>
        <div styleName="product-variant">{/* TODO: variant info must be here */}</div>
      </div>
      <div styleName="price-and-quantity">
        <div styleName="price-block">{props.quantity}</div>
        <div>×</div>
        <div styleName="price-block"><Currency value={props.price} /></div>
      </div>
      <div styleName="product-price">
        <Currency value={props.totalPrice} />
      </div>
      </div>
    </div>
  );
};

export default LineItemRow;
