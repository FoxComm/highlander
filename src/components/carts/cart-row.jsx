import React from 'react';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';


const setCellContents = (order, field) => {
  return _.get(order, field);
};

type Props = {
  cart: Object,
  columns: Array<Object>,
  params: Object,
}

const CartRow = (props: Props) => {
  const { cart, columns, params } = props;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="cart"
      linkParams={{cart: cart.referenceNumber}}
      row={cart}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default CartRow;
