import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import PaymentRow from './row';
import DebitCreditDetails from './debit-credit-details';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

let GiftCardDetails = props => {
  const saveAction = (orderRefNum, amount) => {
    return props
      .editOrderGiftCardPayment(orderRefNum, props.paymentMethod.code, amount);
  };

  return (
    <DebitCreditDetails
      {...props}
      saveAction={saveAction}
    />
  );
};
GiftCardDetails = connect(null, PaymentMethodActions)(GiftCardDetails);

const GiftCard = props => {
  const { amount, code } = props.paymentMethod;
  const orderRefNum = props.order.referenceNumber;

  const deletePayment = () => {
    props.deleteOrderGiftCardPayment(orderRefNum, code);
  };

  const details = editProps => {
    return <GiftCardDetails {...props} {...editProps} />;
  };

  const params = {
    details,
    amount: amount,
    deleteAction: deletePayment,
    ...props,
  };

  return <PaymentRow {...params} />;
};

GiftCard.propTypes = {
  paymentMethod: PropTypes.shape({
    amount: PropTypes.number.isRequired,
    availableBalance: PropTypes.number.isRequired,
    code: PropTypes.string.isRequired
  }),
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderGiftCardPayment: PropTypes.func.isRequired,
  editOrderGiftCardPayment: PropTypes.func.isRequired,
};

export default GiftCard;
