'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerRoles extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  }

  get customer() {
    return this.props.customer;
  }

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-edit"></i>
      </button>
    );
    return (
      <ContentBox title="Roles"
                  className="fc-customer-roles"
                  actionBlock={ actionBlock }>
        <dl>
          <dt>Customer Roles</dt>
          <dd>Customer</dd>
        </dl>
        <dl>
          <dt>Admin Roles</dt>
          <dd>Lorem Ipsum Role</dd>
        </dl>
      </ContentBox>
    );
  }
}
