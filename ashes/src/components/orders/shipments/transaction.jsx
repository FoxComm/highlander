/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';

// styles
import styles from './transaction.css';

// components
import Currency from 'components/common/currency';
import PaymentMethod from 'components/payment/payment-method';
import { DateTime } from 'components/common/datetime';

//types
type Props = {
  source: Object;
  amount: number;
  date: string;
}


const Transaction = (props: Props): Element<*>=> (
  <div styleName="row">
    <div styleName="source">
      <PaymentMethod paymentMethod={props.source} />
    </div>
    <div styleName="date">
      <DateTime value={props.date} />
    </div>
    <div styleName="amount">
      <Currency value={props.amount} />
    </div>
  </div>
);

export default Transaction;
