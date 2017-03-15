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
  return (
    <tr>
      <td styleName="product-image">
        <img src={props.imagePath} />
      </td>
      <td>
        <span styleName="product-info">
          <span styleName="product-name">{props.name}</span>
          <span styleName="product-qty">{props.quantity}</span>
        </span>
      </td>
      <td styleName="product-price">
        <Currency value={props.totalPrice} />
      </td>
    </tr>
  );
};

export default LineItemRow;
