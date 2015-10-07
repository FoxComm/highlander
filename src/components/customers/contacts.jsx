'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerContacts extends React.Component {

  render() {
    return (
      <ContentBox title="ContactInformation" className="fc-customer-contacts">
        Customer contacts data
      </ContentBox>
    );
  }
}
