'use strict';

import React from 'react';

export default class PaymentMethod extends React.Component {
  render() {
    let model = this.props.model;

    return (
      <div className="fc-payment-method">
        <i className={`fc-icon-lg icon-${model.cardType}`}></i>
        <div>
          <div className="fc-strong">{model.cardNumber}</div>
          <div>{model.cardExp}</div>
        </div>
      </div>
    );
  }
}

PaymentMethod.propTypes = {
  model: React.PropTypes.object
};
