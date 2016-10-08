
import React from 'react';
import styles from './summary-line-item.css';

import Currency from 'ui/currency';

const LineItemRow = props => {
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
      <td styleName="product-price"><Currency value={props.totalPrice} /></td>
    </tr>
  );
};

export default LineItemRow;
