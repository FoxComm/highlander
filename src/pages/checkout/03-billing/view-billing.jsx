/* @flow weak */

// libs
import _ from 'lodash';
import React from 'react';

// components
import Icon from 'ui/icon';
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

  const { brand, expMonth, expYear, billingAddress, holderName, lastFour, isDefault } = billingData;
  
  const paymentType = brand ? _.kebabCase(brand) : '';

  const defaultText = isDefault ? <li><div styleName="default-card">Default Card</div></li> : null;
  const lastTwoYear = expYear && expYear.toString().slice(-2);
  const monthYear = expMonth || expYear ?
    <li>{ expMonth }/{ lastTwoYear }</li> : null;
  const addressInfo = !_.isEmpty(billingAddress) ?
    <li><AddressDetails styleName="billing-address" address={billingAddress} /></li> : null;
  const paymentIcon = paymentType ?
    <li><Icon styleName="payment-icon" name={`fc-payment-${paymentType}`} /></li> : null;

  return (
    <ul styleName="view-billing">
      {paymentIcon}
      {defaultText}
      <li styleName="payment-name">{ holderName }</li>
      <li styleName="payment-last-four">{ lastFour }</li>
      {monthYear}
      {addressInfo}
    </ul>
  );
};

export default ViewBilling;
