import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Currency from '../../common/currency';
import PaymentRow from './row';
import DebitCredit from './debit-credit';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

let GiftCardDetails = props => {
  const orderRefNum = props.order.referenceNumber;
  const { amount, availableBalance, code } = props.paymentMethod;

  const handleSave = (amount) => {
    props
      .editOrderGiftCardPayment(orderRefNum, code, amount)
      .then(props.cancelEditing);
  };

  const futureBalance = availableBalance - amount;

  if (!props.isEditing) {
    return (
      <div>
        <dl>
          <dt>Available Balance</dt>
          <dd><Currency value={availableBalance} /></dd>
        </dl>
        <dl>
          <dt>Future Available Balance</dt>
          <dd><Currency value={futureBalance} /></dd>
        </dl>
      </div>
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
  deleteOrderGiftCardPayment: PropTypes.func.isRequired
};

export default GiftCard;
