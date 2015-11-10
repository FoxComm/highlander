import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import { SliderCheckbox } from '../checkbox/checkbox';

export default class CustomerAccountStatus extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired
  }

  render() {
    let customer = this.props.customer;
    return (
      <ContentBox title="Account Status" className="fc-customer-account-status">
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              <strong>Active Account</strong>
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerDisabled" defaultChecked={ !customer.disabled } />
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              <strong>Blacklist Customer</strong>
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerBlacklisted" defaultChecked={ customer.blacklisted } />
            </div>
        </div>
      </ContentBox>
    );
  }
}
