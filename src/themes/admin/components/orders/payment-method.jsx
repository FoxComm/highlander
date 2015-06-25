'use strict';

import React from 'react';

export default class PaymentMethod extends React.Component {
  render() {
    let model = this.props.model;

    return (
      <div className="payment-method">
        <i className={`icon-cc-${model.cardType}`}></i>
        <div>
          <div>{model.cardNumber}</div>
          <div>{model.cardExp}</div>
        </div>
      </div>
    );
  }
}

PaymentMethod.propTypes = {
  model: React.PropTypes.object
};
