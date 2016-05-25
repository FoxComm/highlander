/* @flow */
import React from 'react';
import Currency from '../../common/currency';

type Props = {
  amount: number;
  availableBalance: number;
}

const DebitCreditInfo = (props: Props) => {
  const futureBalance = props.availableBalance - props.amount;

  return (
    <div>
      <dl>
        <dt>Available Balance</dt>
        <dd><Currency value={props.availableBalance} /></dd>
      </dl>
      <dl>
        <dt>Future Available Balance</dt>
        <dd><Currency value={futureBalance} /></dd>
      </dl>
    </div>
  );
};

export default DebitCreditInfo;
