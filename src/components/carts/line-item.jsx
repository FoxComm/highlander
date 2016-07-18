import React, { PropTypes } from 'react';
import Counter from '../forms/counter';
import {DeleteButton} from '../common/buttons';
import Currency from '../common/currency';

const CartLineItem = props => {
  const item = props.item;
  const cart = props.cart;

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
          onChange={({target}) => props.onUpdateCount(cart, item.sku, toNumber(target.value)) }
          decreaseAction={() => props.onUpdateCount(cart, item.sku, item.quantity - 1)}
          increaseAction={() => props.onUpdateCount(cart, item.sku, item.quantity + 1)} />
      </td>
      <td><Currency value={item.totalPrice}/></td>
      <td>
        <DeleteButton onClick={() => props.onStartDelete(item.sku)} />
      </td>
    </tr>
  );
};

CartLineItem.propTypes = {
  onStartDelete: PropTypes.func,
  onUpdateCount: PropTypes.func,
  cart: PropTypes.object,
  item: PropTypes.object
};

export default CartLineItem;
