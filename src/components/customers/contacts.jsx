'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerContacts extends React.Component {

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-edit"></i>
      </button>
    );
    return (
      <ContentBox title="Contact Information"
                  className="fc-customer-contacts"
                  actionBlock={ actionBlock }>
        Customer contacts data
      </ContentBox>
    );
  }
}
