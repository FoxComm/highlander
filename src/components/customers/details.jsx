'use strict';

import React from 'react';
import CustomerContacts from './contacts';
import CustomerAccountPassword from './account-password';
import CustomerRoles from './roles';
import CustomerAddressBook from './address-book';
import CustomerCreditCards from './credit-cards';
import CustomerGroups from './groups';
import CustomerNotificationSettings from './notification-settings';
import CustomerAccountStatus from './account-status';

export default class CustomerDetails extends React.Component {
  render() {
    return (
      <div className="fc-customer-details">
        <div className="gutter">
          <h2>Details</h2>
        </div>
        <div className="gutter">
          <div>
            <CustomerContacts />
          </div>
          <div>
            <CustomerAccountPassword />
            <CustomerRoles />
          </div>
        </div>
        <div className="gutter">
          <CustomerAddressBook />
        </div>
        <div className="gutter">
          <CustomerCreditCards />
        </div>
        <div className="gutter">
          <CustomerGroups />
        </div>
        <div className="gutter">
          <CustomerNotificationSettings />
          <CustomerAccountStatus />
        </div>
      </div>
    );
  }
}
