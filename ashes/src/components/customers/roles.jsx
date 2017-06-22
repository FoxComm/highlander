// libs
import React from 'react';
import PropTypes from 'prop-types';

// components
import ContentBox from '../content-box/content-box';
import Icon from 'components/core/icon';

export default class CustomerRoles extends React.Component {
  static propTypes = {
    customer: PropTypes.object,
  };

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <Icon name="edit" />
      </button>
    );
    return (
      <ContentBox title="Roles" className="fc-customer-roles" actionBlock={actionBlock}>
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
