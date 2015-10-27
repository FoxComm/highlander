'use strict';

import React, { PropTypes } from 'react';
import CustomerContacts from './contacts';
import CustomerAccountPassword from './account-password';
import CustomerRoles from './roles';
import CustomerAddressBook from './address-book';
import CustomerCreditCards from './credit-cards';
import CustomerGroups from './groups';
import CustomerNotificationSettings from './notification-settings';
import CustomerAccountStatus from './account-status';

export default class CustomerDetails extends React.Component {

  static propTypes = {
    entity: PropTypes.object
  }

  get customer() {
    return this.props.entity;
  }

  render() {
    return (
      <div className="fc-customer-details">
        <div className="gutter">
          <h2>Details</h2>
        </div>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <CustomerContacts customer={this.customer} />
          </div>
          <div className="fc-col-md-1-2">
            <CustomerAccountPassword />
            <CustomerRoles />
          </div>
        </div>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <CustomerAddressBook customerId={this.customer.id} />
          </div>
        </div>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <CustomerCreditCards customerId={this.customer.id} />
          </div>
        </div>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <CustomerGroups />
          </div>
        </div>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <CustomerNotificationSettings />
          </div>
          <div className="fc-col-md-1-2">
            <CustomerAccountStatus customer={this.customer}/>
          </div>
        </div>
      </div>
    );
  }
}
