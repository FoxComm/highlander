'use strict';

import React, { PropTypes } from 'react';

export default class PaymentMethod extends React.Component {

  static propTypes = {
    model: PropTypes.object
  }

  render() {
    let model = this.props.model;

    return (
      <div className="fc-payment-method">
        <div className="fc-left">
          <i className={`fc-icon-lg icon-${model.cardType}`}></i>
        </div>
        <div className="fc-left">
          <div className="fc-strong">{model.cardNumber}</div>
          <div>{model.cardExp}</div>
        </div>
      </div>
    );
  }
}
