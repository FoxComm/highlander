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
    return (
      <div>
        <div className='fc-shipping-method-input-price fc-left'>
          <CurrencyInput defaultValue={shippingMethod.price} />
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
        <div className='fc-shipping-method-row-price-field'>
          <Currency value={shippingMethod.price} />
        </div>
        <div className='fc-right'>
          {shippingMethod.isSelected ? <EditButton onClick={editPriceAction} /> : null}
        </div>
      </div>
    );
  }
};

const ShippingMethodRow = props => {
  const { shippingMethod, updateAction, isEditingPrice, editPriceAction, cancelPriceAction, ...rest} = props;
  return (
    <TableRow {...rest} >
      <TableCell>
        <RadioButton className='fc-shipping-method-row-name-control' checked={shippingMethod.isSelected} onChange={updateAction}>
          <span className='fc-shipping-method-row-name-field'>{shippingMethod.name}</span>
        </RadioButton>
      </TableCell>
      <TableCell>
        {editBlock(shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction)}
      </TableCell>
    </TableRow>
  );
};

export default ShippingMethodRow;

