/* @flow */
import React from 'react';

import DebitCredit from './debit-credit';
import DebitCreditInfo from './debit-credit-info';

type Props = {
  orderReferenceNumber: string,
  paymentMethod: {
    amount: number,
    availableBalance: number;
  };
  saveAction: (orderRefNum: string, amount: number) => Promise<*>;
  handleCancel: () => void;
  isEditing: boolean;
}

const DebitCreditDetails = (props: Props) => {
  const orderRefNum = props.orderReferenceNumber;
  const { amount, availableBalance } = props.paymentMethod;

  const handleSave = (amount) => {
    props
      .saveAction(orderRefNum, amount)
      .then(props.handleCancel);
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
        onCancel={props.handleCancel}
        onSubmit={handleSave}
        saveText="Save"
      />
    );
  }
};

export default DebitCreditDetails;
