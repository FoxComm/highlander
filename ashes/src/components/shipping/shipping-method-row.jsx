// libs
import React from 'react';
import PropTypes from 'prop-types';

// components
import Currency from '../common/currency';
import { EditButton } from 'components/core/button';
import CurrencyInput from '../forms/currency-input';
import RadioButton from 'components/core/radio-button';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import SaveCancel from 'components/core/save-cancel';

const editBlock = (shippingMethod, isEditingPrice, editPriceAction, cancelPriceAction, submitPriceAction) => {
  if (shippingMethod.isSelected && isEditingPrice) {
    return (
      <div>
        <div className="fc-shipping-method-input-price fc-left">
          <CurrencyInput defaultValue={shippingMethod.price} />
        </div>
        <SaveCancel
          className="fc-right"
          onCancel={cancelPriceAction}
          onSave={() => submitPriceAction(shippingMethod.id)}
        />
      </div>
    );
  }

  return (
    <div>
      <div className="fc-shipping-method-row-price-field">
        <Currency value={shippingMethod.price} />
      </div>
      <div className="fc-right">
        {shippingMethod.isSelected ? <EditButton onClick={editPriceAction} /> : null}
      </div>
    </div>
  );
};

const ShippingMethodRow = props => {
  const {
    shippingMethod,
    updateAction,
    isEditingPrice,
    editPriceAction,
    cancelPriceAction,
    submitPriceAction,
    ...rest
  } = props;

  const inputId = `fc-shipping-method-${shippingMethod.id}`;

  return (
    <TableRow {...rest}>
      <TableCell>
        <RadioButton
          id={inputId}
          label={shippingMethod.name}
          className="fc-shipping-method-row-name-control"
          checked={shippingMethod.isSelected}
          onChange={updateAction}
        />
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
  submitPriceAction: PropTypes.func,
};

export default ShippingMethodRow;
