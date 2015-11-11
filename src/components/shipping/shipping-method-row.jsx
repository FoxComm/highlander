import React from 'react';
import TableRow from '../table/row';
import TableCell from '../table/cell';

const columns = [
  { field: 'name', text: 'Method' },
  { field: 'price', text: 'Price', type: 'currency' }
];

const ShippingMethodRow = (props) => {
  const { shippingMethod, isSelected, onSelect, ...rest} = props;

  return (
    <TableRow {...rest} >
      <TableCell onClick={onSelect}>
        <div className='contents'>
          <input type='radio' className='name-control' />
          <span className='name-field'>{shippingMethod.name}</span>
        </div>
      </TableCell>
      <TableCell>{shippingMethod.price}</TableCell>
    </TableRow>
  );
};

export default ShippingMethodRow;

