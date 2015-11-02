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
import SectionSubtitle from '../section-title/section-subtitle';

const CustomerDetails = (props) => {
  const customer = props.entity;
  return (
    <div className="fc-customer-details">
      <SectionSubtitle title="Details" />
      <div className="fc-grid">
        <div className="fc-col-md-1-2">
          <CustomerContacts customer={ customer } />
        </div>
        <div className="fc-col-md-1-2">
          <CustomerAccountPassword />
          <CustomerRoles />
        </div>
      </div>
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <CustomerAddressBook customerId={ customer.id } />
        </div>
      </div>
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <CustomerCreditCards customerId={ customer.id } />
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
          <CustomerAccountStatus customer={ customer }/>
        </div>
      </div>
    </div>
  );
};

CustomerDetails.propTypes = {
    entity: PropTypes.object
};

export default CustomerDetails;
