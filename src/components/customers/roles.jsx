'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerRoles extends React.Component {

  render() {
    // return (<div className="fc-customer-contacts">Customer roles</div>);
    return (
      <ContentBox title="Roles" className="fc-customer-roles">
        Roles
      </ContentBox>
    );
  }
}
