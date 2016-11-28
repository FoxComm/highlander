import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import ContentBox from '../content-box/content-box';
import { SliderCheckbox } from '../checkbox/checkbox';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';
import ConfirmationDialog from '../modal/confirmation-dialog';


@connect((state, props) => ({
  ...state.customers.details
}), CustomersActions)
export default class CustomerAccountStatus extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired,
    toggleDisableStatus: PropTypes.func.isRequired,
    startDisablingCustomer: PropTypes.func.isRequired,
    stopDisablingCustomer: PropTypes.func.isRequired,
    toggleBlacklisted: PropTypes.func.isRequired,
    startBlacklistCustomer: PropTypes.func.isRequired,
    stopBlacklistCustomer: PropTypes.func.isRequired,
    isDisablingStarted: PropTypes.bool.isRequired,
    isBlacklistedStarted: PropTypes.bool.isRequired
  };

  get customerInfo() {
    const customer = this.props.customer;
    return (
      <ul className="fc-customer-disable-confirm-customer">
        <li><strong>{customer.name}</strong></li>
        <li>{customer.email}</li>
        <li>{customer.phoneNumber}</li>
      </ul>
    );
  }

  get disableOptions() {
    const customer = this.props.customer;
    if (customer.disabled) {
      return {
        header: 'Activate Customer Account',
        body: (
          <div className="fc-customer-disable-confirm">
            <div>Are you sure you want to active the account for the following customer?</div>
            {this.customerInfo}
            <div>You can deactivate this account at anytime.</div>
          </div>
        ),
        confirm: 'Yes, Activate Account',
        cancel: 'Cancel'
      };
    } else {
      return {
        header: 'Deactivate Customer Account',
        body: (
          <div className="fc-customer-disable-confirm">
            <div>Are you sure you want to deactivate the account for the following customer?</div>
            {this.customerInfo}
            <div>You can reactivate this account at anytime.</div>
          </div>
        ),
        confirm: 'Yes, Deactivate Account',
        cancel: 'Cancel'
      };
    }
  }

  get blacklistedOptions() {
    const customer = this.props.customer;
    if (customer.isBlacklisted) {
      return {
        header: 'Remove Customer From Blacklist',
        body: (
          <div className="fc-customer-blacklist-confirm">
            <div>Are you sure you want to remove the following customer from the Blacklist?</div>
            {this.customerInfo}
            <div>You can place this customer on the Blacklist at anytime.</div>
          </div>
        ),
        confirm: 'Yes, Remove',
        cancel: 'Cancel'
      };
    } else {
      return {
        header: 'Blacklist Customer',
        body: (
          <div className="fc-customer-blacklist-confirm">
            <div>Are you sure you want to place the following custom on the Blacklist?</div>
            {this.customerInfo}
            <div>You can take this customer off the Blacklist at anytime.</div>
          </div>
        ),
        confirm: 'Yes, Blacklist',
        cancel: 'Cancel'
      };
    }
  }

  render() {
    let customer = this.props.customer;
    return (
    <div>
      <ContentBox title="Account Status" className="fc-customer-account-status">
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              <strong>Active Account</strong>
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" onChange={this.props.startDisablingCustomer}
                              id="customerDisabled" checked={ !customer.disabled } />
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              <strong>Blacklist Customer</strong>
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" onChange={this.props.startBlacklistCustomer}
                              id="customerBlacklisted" checked={ customer.isBlacklisted } />
            </div>
        </div>
        <ConfirmationDialog
          {...this.disableOptions}
          isVisible={this.props.isDisablingStarted}

          confirmAction={() => {
            const customer = this.props.customer;
            this.props.toggleDisableStatus(customer.id, !customer.disabled);
          }}
          onCancel={this.props.stopDisablingCustomer}/>
        <ConfirmationDialog
          {...this.blacklistedOptions}
          isVisible={this.props.isBlacklistedStarted}
          confirmAction={() => {
            const customer = this.props.customer;
            this.props.toggleBlacklisted(customer.id, !customer.isBlacklisted);
          }}
          onCancel={this.props.stopBlacklistCustomer}/>
      </ContentBox>
    </div>
    );
  }
}
