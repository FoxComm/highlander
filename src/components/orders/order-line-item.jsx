'use strict';

import React, { PropTypes} from 'react';
import { formatCurrency } from '../../lib/format';
import Counter from '../forms/counter';
import DeleteButton from '../common/delete-button';

const OrderLineItem = (props) => {
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
        <Counter
          inputName={`line-item-quantity-${item.sku}`}
          value={item.quantity}
          minValue={0}
          maxValue={1000000}
          stepAmount={1}
          onChange={handleChange}
          decreaseTotal={() => props.updateLineItemCount(order, item.sku, item.quantity - 1)}
          increaseTotal={() => props.updateLineItemCount(order, item.sku, item.quantity + 1)} />
      </td>
      <td>{formatCurrency(item.totalPrice)}</td>
      <td>
        <DeleteButton onClick={() => props.orderLineItemsStartDelete(item.sku)} />
      </td>
    </tr>
  );
};

export default OrderLineItem;