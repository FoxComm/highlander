// libs
import React, { PropTypes } from 'react';

// components
import Currency from '../common/currency';
import { EditButton, PrimaryButton } from '../common/buttons';
import CurrencyInput from '../forms/currency-input';
import RadioButton from '../forms/radio-button';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import SaveCancel from '../common/save-cancel';

const editBlock = (shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction, submitPriceAction) => {
  if (shippingMethod.isSelected && isEditingPrice) {
    return (
      <div>
        <div className='fc-shipping-method-input-price fc-left'>
          <CurrencyInput defaultValue={shippingMethod.price} />
        </div>
        <SaveCancel className="fc-right"
                    onCancel={cancelPriceAction}
                    cancelClassName="fc-action-block-cancel"
                    onSave={() => submitPriceAction(shippingMethod.id)} />
      </div>
    );
  }

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
};

const ShippingMethodRow = props => {
  const { shippingMethod, updateAction, isEditingPrice, editPriceAction, cancelPriceAction, submitPriceAction, ...rest} = props;
  return (
    <TableRow {...rest} >
      <TableCell>
        <RadioButton className='fc-shipping-method-row-name-control'
                     checked={shippingMethod.isSelected}
                     onChange={updateAction}>
          <span className='fc-shipping-method-row-name-field'>{shippingMethod.name}</span>
        </RadioButton>
      </TableCell>
      <TableCell>
        {editBlock(shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction, submitPriceAction)}
      </TableCell>
    </TableRow>
  );
};

ShippingMethodRow.propTypes = {
  shippingMethod: PropTypes.object,
  updateAction: PropTypes.func,
  isEditingPrice: PropTypes.bool,
  editPriceAction: PropTypes.func,
  cancelPriceAction: PropTypes.func,
  submitPriceAction: PropTypes.func
};

export default ShippingMethodRow;
