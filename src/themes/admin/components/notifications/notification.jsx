'use strict';

import React from 'react';
import { Link } from 'react-router';
import ContactMethod from './method';

class Notification extends React.Component {
  render() {
    let notification = this.props.notification;

    return (
      <div className='modal resend-notification'>
        <div className='modal-header'>
          Resend Email?
        </div>
        <div className='modal-body'>
          You will send another copy of this email to:
          <ContactMethod type={notification.contactType} value={notification.contact} />
        </div>
        <div className='modal-footer'>
          <Link to="notifications" params={{order: notification.order}}>Cancel</Link>
          <a href="">Resend</a>
        </div>
      </div>
    );
  }
}

Notification.propTypes = {
  notification: React.PropTypes.object
};

Notification.defaultProps = {
  notification: {
    id: 42,
    date: new Date().toISOString(),
    status: 'Delivered',
    subject: 'Order confirmed',
    contactType: 'SMS',
    contact: 'jim@bob.com',
    order: 1234
  }
};

export default Notification;
