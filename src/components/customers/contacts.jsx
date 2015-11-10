import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerContacts extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  }

  render() {
    let customer = this.props.customer;
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-edit"></i>
      </button>
    );
    return (
      <ContentBox title="Contact Information"
                  className="fc-customer-contacts"
                  actionBlock={ actionBlock }>
        <dl>
          <dt>Name</dt>
          <dd>{ customer.name }</dd>
        </dl>
        <dl>
          <dt>Email Address</dt>
          <dd>{ customer.email }</dd>
        </dl>
        <dl>
          <dt>Phone Number</dt>
          <dd>{ customer.phoneNumber }</dd>
        </dl>
      </ContentBox>
    );
  }
}
