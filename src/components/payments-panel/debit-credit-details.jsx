/* @flow */
import React from 'react';

import DebitCredit from './debit-credit';
import DebitCreditInfo from './debit-credit-info';

type Props = {
  order: {
    referenceNumber: string,
  };
  paymentMethod: {
    amount: number,
    availableBalance: number;
  };
  saveAction: (orderRefNum: string, amount: number) => Promise;
  cancelEditing: () => void;
  isEditing: boolean;
}

const DebitCreditDetails = (props: Props) => {
  const orderRefNum = props.order.referenceNumber;
  const { amount, availableBalance } = props.paymentMethod;

  const handleSave = (amount) => {
    props
      .saveAction(orderRefNum, amount)
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

export default DebitCreditDetails;
