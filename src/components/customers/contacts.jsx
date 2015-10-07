'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerContacts extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired
  }

  get customer() {
    return this.props.customer;
  }

  render() {
    console.log("CustomerContacts");
    console.log(this.props);

    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-edit"></i>
      </button>
    );
    return (
      <ContentBox title="Contact Information"
                  className="fc-customer-contacts"
                  actionBlock={ actionBlock }>
        <dl>
          <dt>First Name</dt>
          <dd>{ this.customer.firstName }</dd>
        </dl>
        <dl>
          <dt>Last Name</dt>
          <dd>{ this.customer.lastName }</dd>
        </dl>
        <dl>
          <dt>Email Address</dt>
          <dd>{ this.customer.email }</dd>
        </dl>
        <dl>
          <dt>Phone Number</dt>
          <dd>{ this.customer.phoneNumber }</dd>
        </dl>
      </ContentBox>
    );
  }
}
