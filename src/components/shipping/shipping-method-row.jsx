import React from 'react';
import Currency from '../common/currency';
import { EditButton, PrimaryButton } from '../common/buttons';
import PrependMoneyInput from '../forms/prepend-money-input';
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
      <div className='contents'>
        <div className='shipping-method-input-price'>
          <PrependMoneyInput value={shippingMethod.price} />
        </div>
        <div className='shipping-method-action'>
          <a className='shipping-cancel-action' onClick={cancelPriceAction}>Cancel</a>
          <PrimaryButton onClick={() => submitPriceAction(shippingMethod.id)}>Save</PrimaryButton>
        </div>
      </div>
    );
  } else {
    return (
      <div className='contents'>
        <div className='shipping-method-price'>
          <Currency value={shippingMethod.price} />
        </div>
        <div className='shipping-method-action'>
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
        <div className='contents'>
          <RadioButton className='name-control' checked={shippingMethod.isSelected} onClick={updateAction}>
            <span className='name-field'>{shippingMethod.name}</span>
          </RadioButton>
        </div>
      </TableCell>
      <TableCell>
        {editBlock(shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction)}
      </TableCell>
    </TableRow>
  );
};

export default ShippingMethodRow;

