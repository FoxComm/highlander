/* @flow */

import React, { Element } from 'react';
import Counter from 'components/forms/counter';
import {DeleteButton} from 'components/common/buttons';
import Currency from 'components/common/currency';

type Props = {
  onStartDelete: Function,
  onUpdateCount: Function,
  cart: {
    referenceNumber: string,
  },
  item: {
    imagePath: srting,
    name: string,
    sku: string,
    price: number,
    totalPrice: number,
    quantity: number,
  },
};

const CartLineItem = (props: Props): Element => {
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

export default CartLineItem;
