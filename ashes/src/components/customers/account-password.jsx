import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerAccountPassword extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  };

  render() {
    return (
      <ContentBox title="Account Password" className="fc-customer-account-password">
        <button id="customer-reset-password-btn" className="fc-btn">Reset Password</button>
      </ContentBox>
    );
  }
}
