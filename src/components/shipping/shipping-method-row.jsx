import React from 'react';
import formatCurrency from '../../lib/format-currency';
import Currency from '../common/currency';
import { EditButton, PrimaryButton } from '../common/buttons';
import CurrencyInput from '../forms/currency-input';
import RadioButton from '../forms/radio-button';
import TableRow from '../table/row';
import TableCell from '../table/cell';

const columns = [
  { field: 'name', text: 'Method' },
  { field: 'price', text: 'Price', type: 'currency' }
];

const editBlock = (shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction, submitPriceAction) => {
  if (shippingMethod.isSelected && isEditingPrice) {
    const price = formatCurrency(shippingMethod.price, 100, '');

    return (
      <div className='contents'>
        <div className='shipping-method-input-price'>
          <CurrencyInput value={price} />
        </div>
        <div className='fc-right'>
          <a className='fc-action-block-cancel' onClick={cancelPriceAction}>Cancel</a>
          <PrimaryButton onClick={() => submitPriceAction(shippingMethod.id)}>Save</PrimaryButton>
        </div>
      </div>
    );
  } else {
    return (
      <div>
        <div className='shipping-method-row-price-field'>
          <Currency value={shippingMethod.price} />
        </div>
        <div className='fc-right'>
          {shippingMethod.isSelected ? <EditButton onClick={editPriceAction} /> : null}
        </div>
      </div>
    );
  }
};

const ShippingMethodRow = (props) => {
  const { shippingMethod, updateAction, isEditingPrice, editPriceAction, cancelPriceAction, ...rest} = props;
  return (
    <TableRow {...rest} >
      <TableCell>
        <RadioButton className='shipping-method-row-name-control' checked={shippingMethod.isSelected} onClick={updateAction}>
          <span className='shipping-method-row-name-field'>{shippingMethod.name}</span>
        </RadioButton>
      </TableCell>
      <TableCell>
        {editBlock(shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction)}
      </TableCell>
    </TableRow>
  );
};

export default ShippingMethodRow;

