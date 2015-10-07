'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerAddressBook extends React.Component {

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );
    return (
      <ContentBox title="Address Book"
                  className="fc-customer-address-book"
                  actionBlock={ actionBlock }>
        Address book
      </ContentBox>
    );
  }
}
