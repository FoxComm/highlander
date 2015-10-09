'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerAccountStatus extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired
  }

  render() {
    let customer = this.props.customer;
    return (
      <ContentBox title="Account Status" className="fc-customer-account-status">
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-2-3">
              <strong>Active Account</strong>
            </div>
            <div className="fc-col-1-3">
              <input type="checkbox" defaultChecked={ !customer.disabled } className="fc-right"/>
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-2-3">
              <strong>Blacklist Customer</strong>
            </div>
            <div className="fc-col-1-3">
              <input type="checkbox" defaultChecked={ customer.blacklisted } className="fc-right"/>
            </div>
        </div>
      </ContentBox>
    );
  }
}
