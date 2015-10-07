'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerCreditCards extends React.Component {

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );
    return (
      <ContentBox title="Credit Cards"
                  className="fc-customer-credit-cards"
                  actionBlock={ actionBlock }>
        Credit cards
      </ContentBox>
    );
  }
}
