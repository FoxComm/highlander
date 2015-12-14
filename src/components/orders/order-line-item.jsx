import React, { PropTypes } from 'react';
import Counter from '../forms/counter';
import {DeleteButton} from '../common/buttons';
import Currency from '../common/currency';

const OrderLineItem = props => {
  const item = props.item;
  const order = props.order.currentOrder;

  const toNumber = value => {
    return value ? parseInt(value, 10) : 1;
  };

  return (
    <tr>
      <td><img src={item.imagePath} /></td>
      <td>{item.name}</td>
      <td>{item.sku}</td>
      <td><Currency value={item.price}/></td>
      <td>
        <Counter
          id={`line-item-quantity-${item.sku}`}
          value={item.quantity}
          min={0}
          max={1000000}
          step={1}
          onChange={({target}) => props.updateLineItemCount(order, item.sku, toNumber(target.value)) }
          decreaseAction={() => props.updateLineItemCount(order, item.sku, item.quantity - 1)}
          increaseAction={() => props.updateLineItemCount(order, item.sku, item.quantity + 1)} />
      </td>
      <td><Currency value={item.totalPrice}/></td>
      <td>
        <DeleteButton onClick={() => props.orderLineItemsStartDelete(item.sku)} />
      </td>
    </tr>
  );
};

OrderLineItem.propTypes = {
  orderLineItemsStartDelete: PropTypes.func,
  order: PropTypes.shape({
    currentOrder: PropTypes.object
  }),
  item: PropTypes.object
};

export default OrderLineItem;
