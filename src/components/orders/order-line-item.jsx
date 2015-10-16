'use strict';

import React, { PropTypes} from 'react';
import { formatCurrency } from '../../lib/format';
import DeleteButton from '../common/delete-button';

let OrderLineItem = (props) => {
  let item = props.item;
  let order = props.order.currentOrder;

  let handleChange = (event) => {
    props.updateLineItemCount(order, item.sku, event.target.value);
  };

  return (
    <tr>
      <td><img src={item.imagePath} /></td>
      <td>{item.name}</td>
      <td>{item.sku}</td>
      <td>{formatCurrency(item.price)}</td>
      <td>
        <div className="fc-input-group fc-counter">
          <div className="fc-input-prepend">
            <button onClick={() => props.updateLineItemCount(order, item.sku, item.quantity - 1)}>
              <i className="icon-chevron-down"></i>
            </button>
          </div>
          <input
            type='number'
            name={`line-item-quantity-${item.sku}`}
            value={item.quantity}
            min={0}
            max={10000000}
            step={1}
            onChange={handleChange} />
          <div className="fc-input-append">
            <button onClick={() => props.updateLineItemCount(order, item.sku, item.quantity + 1)}>
              <i className="icon-chevron-up"></i>
            </button>
          </div>
        </div>
      </td>
      <td>{formatCurrency(item.totalPrice)}</td>
      <td>
        <DeleteButton onClick={() => props.orderLineItemsStartDelete(item.sku)} />
      </td>
    </tr>
  );
};

export default OrderLineItem;