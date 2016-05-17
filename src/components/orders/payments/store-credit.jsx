import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import PaymentRow from './row';
import DebitCredit from './debit-credit';
import DebitCreditInfo from './debit-credit-info';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

let StoreCreditDetails = props => {
  const orderRefNum = props.order.referenceNumber;
  const { amount, availableBalance } = props.paymentMethod;

  const handleSave = (amount) => {
    props
      .editOrderStoreCreditPayment(orderRefNum, amount)
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
StoreCreditDetails = connect(null, PaymentMethodActions)(StoreCreditDetails);

const StoreCredit = props => {
  const { amount } = props.paymentMethod;

  const orderRefNum = props.order.referenceNumber;

  const deletePayment = () => {
    props.deleteOrderStoreCreditPayment(orderRefNum);
  };

  const details = editProps => {
    return <StoreCreditDetails {...props} {...editProps} />;
  };

  const params = {
    details,
    amount,
    deleteAction: deletePayment,
    ...props,
  };

  return <PaymentRow {...params} />;
};

StoreCredit.propTypes = {
  paymentMethod: PropTypes.shape({
    amount: PropTypes.number.isRequired,
    availableBalance: PropTypes.number.isRequired
  }),
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderStoreCreditPayment: PropTypes.func.isRequired,
  editOrderStoreCreditPayment: PropTypes.func.isRequired,
};

export default StoreCredit;
