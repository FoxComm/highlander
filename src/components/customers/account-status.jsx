import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import ContentBox from '../content-box/content-box';
import { SliderCheckbox } from '../checkbox/checkbox';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';


@connect((state, props) => ({
  ...state.customers.status
}), CustomersActions)
export default class CustomerAccountStatus extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired,
    toggleDisableStatus: PropTypes.func.isRequired
  }

  @autobind
  onActiveChange(event) {
    const customer = this.props.customer;
    this.props.toggleDisableStatus(customer.id, !customer.disabled);
    return true;
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
              <SliderCheckbox className="fc-right" onChange={this.onActiveChange}
                              id="customerDisabled" checked={ !customer.disabled } />
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
