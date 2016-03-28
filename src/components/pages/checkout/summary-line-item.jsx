
import React from 'react';
import styles from './summary-line-item.css';

import Currency from 'ui/currency';

const LineItemRow = props => {
  return (
    <tr>
      <td styleName="product-image">
        <img src={props.imagePath} />
      </td>
      <td styleName="product-name">{props.name}</td>
      <td styleName="product-qty">{props.quantity}</td>
      <td styleName="product-price"><Currency value={props.totalPrice} /></td>
    </tr>
  );
};

export default LineItemRow;
