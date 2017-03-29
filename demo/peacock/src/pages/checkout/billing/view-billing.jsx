/* @flow weak */

// libs
import _ from 'lodash';
import React from 'react';

// components
import { AddressDetails } from 'ui/address';

// styles
import styles from './view-billing.css';

// types
import type { BillingData } from '../types';

type Props = {
  billingData: ?BillingData,
};

const ViewBilling = (props: Props) => {
  const { billingData } = props;
  if (!billingData || _.isEmpty(billingData)) return null;

  const { brand, expMonth, expYear, address, holderName, lastFour, isDefault } = billingData;

  const getAddress = () => {
    if (!_.isEmpty(address)) {
      return (
        <li styleName="billing-address">
          Billing address: <AddressDetails styleName="billing-address" address={address} />
        </li>
      );
    }
  };

  const paymentType = brand ? _.upperCase(brand) : '';

  const defaultText = isDefault ? <li><div styleName="default-card">Default Card</div></li> : null;
  const lastTwoYear = expYear && expYear.toString().slice(-2);
  const monthYear = expMonth && expYear ?
    `${expMonth < 10 ? `0${expMonth}` : expMonth}/${lastTwoYear}` : 'xx/xx';
  const addressInfo = getAddress();

  return (
    <ul styleName="view-billing">
      <li styleName="payment-name">{ `${holderName}'s ${paymentType}` }</li>
      <li styleName="payment-last-four">{ `Ending in ${lastFour}, expires ${monthYear}` }</li>
      {defaultText}
      {addressInfo}
    </ul>
  );
};

export default ViewBilling;
