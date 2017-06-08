import React from 'react';
import PropTypes from 'prop-types';
import ContentBox from 'components/core/content-box';

export default class CustomerAccountPassword extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  };

  render() {
    return (
      <ContentBox title="Account Password" className="fc-customer-account-password">
        <button id="customer-reset-password-btn" className="fc-btn fc-btn-reset-password">Reset Password</button>
      </ContentBox>
    );
  }
}
