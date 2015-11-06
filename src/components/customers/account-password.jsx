'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerAccountPassword extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  }

  render() {
    return (
      <ContentBox title="Account Password" className="fc-customer-account-password">
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
          </div>
          <div className="fc-col-md-1-2">
            <button className="fc-btn fc-right">Reset Password</button>
          </div>
        </div>
      </ContentBox>
    );
  }
}
