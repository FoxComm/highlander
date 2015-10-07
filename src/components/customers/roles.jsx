'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerRoles extends React.Component {

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
        Roles
      </ContentBox>
    );
  }
}
