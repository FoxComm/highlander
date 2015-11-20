import _ from 'lodash';
import React, { PropTypes } from 'react';
import * as CardUtils from '../../lib/credit-card-utils';
import static_url from '../../lib/s3';

export default class PaymentMethod extends React.Component {
  render() {
    const card = this.props.card;

    return (
      <div className="fc-payment-method">
        <div className="fc-left">
          <img className="fc-icon-lg" src={static_url(`images/payments/payment_${card.brand.toLowerCase()}.png`)}></img>
        </div>
        <div className="fc-left">
          <div className="fc-strong">{CardUtils.formatNumber(card)}</div>
          <div>{CardUtils.formatExpiration(card)}</div>
        </div>
      </div>
    );
  }
}

PaymentMethod.propTypes = {
  card: PropTypes.object.isRequired
};
