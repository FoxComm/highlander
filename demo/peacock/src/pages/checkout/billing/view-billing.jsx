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
  inModal?: boolean,
};

const ViewBilling = (props: Props) => {
  const { billingData } = props;
  if (!billingData || _.isEmpty(billingData)) return null;

  const { brand, expMonth, expYear, address, holderName, lastFour, isDefault } = billingData;

  const getAddress = () => {
    if (_.isEmpty(address) || props.inModal) return null;

    return (
      <li styleName="billing-address">
        <AddressDetails styleName="billing-address" address={address} />
      </li>
    );
  };

  const paymentType = brand ? _.upperCase(brand) : '';

  const defaultText = isDefault ? <span styleName="default-card">(Default)</span> : null;
  const lastTwoYear = expYear && expYear.toString().slice(-2);
  const monthYear = expMonth && expYear ?
    `${expMonth < 10 ? `0${expMonth}` : expMonth}/${lastTwoYear}` : 'xx/xx';
  const addressInfo = getAddress();

  return (
    <ul styleName="view-billing">
      <li styleName="payment-name">
        <span styleName="name">{holderName}</span>
        { `'s ${paymentType}`}{defaultText}
      </li>
      <li styleName="payment-last-four">{ `Ending in ${lastFour}, expires ${monthYear}` }</li>
      {addressInfo}
    </ul>
  );
};

export default ViewBilling;
