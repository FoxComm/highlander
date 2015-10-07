'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerAddressBook extends React.Component {

  render() {
    return (
      <ContentBox title="Address Book" className="fc-customer-address-book">
        Address book
      </ContentBox>
    );
  }
}
