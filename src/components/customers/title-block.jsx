'use strict';

import React, { PropTypes } from 'react';

export default class Customer extends React.Component {
  static propTypes ={
    customer: PropTypes.object.isRequired
  }

  get customer {
    return this.props.customer;
  }

  render() {
    return (
      <div className="fc-content-box">
        <div className="fc-customer-info-header">
          <div className="fc-customer-info-head">
            <div className="fc-customer-info-rank">
              {this.customer.rank}
            </div>
          </div>
          <div className="fc-customer-info-avatar">
            <i className="icon-customer"></i>
          </div>
          <div className="fc-customer-info-name">
            {this.customer.firstName} {this.customer.lastName}
          </div>
          <div className="fc-customer-info-email">
            {this.customer.email}
          </div>
        </div>
      </div>
    );
  }
}
