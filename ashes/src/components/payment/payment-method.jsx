/* @flow */

import _ from 'lodash';
import React from 'react';
import * as CardUtils from '../../lib/credit-card-utils';
import static_url from '../../lib/s3';

import GiftCardCode from '../gift-cards/gift-card-code';

import type { PaymentMethod as TPaymentMethod } from 'paragons/order';

import styles from './payment-method.css';

type Props = {
  paymentMethod: TPaymentMethod,
  type?: string,
  className?: string,
};

function getIconType(type, paymentMethod) {
  switch (type && paymentMethod.brand) {
    case 'creditCard':
      return paymentMethod.brand.toLowerCase().replace(/ /g, '');
    default:
      return null;
  }
}

function getTitle(type, paymentMethod) {
  switch (type) {
    case 'creditCard':
      return CardUtils.formatNumber(paymentMethod);
    default:
      return _.startCase(type);
  }
}

function getSubtitle(type, paymentMethod) {
  switch (type) {
    case 'creditCard':
      return CardUtils.formatExpiration(paymentMethod);
    case 'giftCard':
      return <GiftCardCode value={paymentMethod.code} />;
    default:
      return null;
  }
}

const PaymentMethod = (props: Props) => {
  const { paymentMethod } = props;

  const type = _.get(props, 'paymentMethod.type', 'creditCard');
  const iconType = getIconType(type, paymentMethod);

  let icon;

  if (iconType) {
    icon = (
      <img
        styleName="payment-icon"
        className="fc-icon-lg"
        src={static_url(`images/payments/payment_${iconType}.svg`)}
      />
    );
  }

  return (
    <div className={props.className} styleName="payment-method">
      {icon}
      <div styleName="payment-summary">
        <strong>{getTitle(type, paymentMethod)}</strong>
        <div>{getSubtitle(type, paymentMethod)}</div>
      </div>
    </div>
  );
};

export default PaymentMethod;
