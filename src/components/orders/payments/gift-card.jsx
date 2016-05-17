import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import PaymentRow from './row';
import DebitCredit from './debit-credit';
import DebitCreditInfo from './debit-credit-info';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

let GiftCardDetails = props => {
  const orderRefNum = props.order.referenceNumber;
  const { amount, availableBalance, code } = props.paymentMethod;

  const handleSave = (amount) => {
    props
      .editOrderGiftCardPayment(orderRefNum, code, amount)
      .then(props.cancelEditing);
  };

  if (!props.isEditing) {
    return (
      <DebitCreditInfo
        availableBalance={availableBalance}
        amount={amount}
      />
    );
  } else {
    return (
      <DebitCredit
        amountToUse={amount}
        availableBalance={availableBalance}
        onCancel={props.cancelEditing}
        onSubmit={handleSave}
        saveText="Save"
      />
    );
  }
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
