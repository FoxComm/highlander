/* @flow weak */

// libs
import _ from 'lodash';
import React from 'react';

// components
import Currency from 'ui/currency';

// styles
import styles from './view-billing.css';

type Props = {
  paymentMethods: Array<any>,
};

const ViewGiftCards = (props: Props) => {
  const giftCards = _.map(props.paymentMethods, method => {
    if (method.type == 'giftCard') {
      return (
        <li styleName="payment-gift">
          GIFT CARD { method.code }
          <Currency styleName="price" value={ method.currentBalance } />
        </li>
      );
    }
  });

  return (_.isEmpty(giftCards)) ? null : (
    <ul styleName="view-billing">
      { giftCards }
    </ul>
  );
};

export default ViewGiftCards;
