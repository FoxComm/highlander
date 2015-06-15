'use strict';

import React from 'react';
import { Link } from 'react-router';
import ContactMethod from './method';

class Notification extends React.Component {
  render() {
    let notification = this.props.notification;

    return (
      <div className='modal resend-notification'>
        <form action="POST">
          <div className='modal-header'>
            <div>
              <i className='icon-attention'></i>
            </div>
            <div>Resend Email?</div>
          </div>
          <div className='modal-body'>
            You will send another copy of this email to:
            <ContactMethod type={notification.contactType} value={notification.contact} />
          </div>
          <div className='modal-footer'>
            <Link to="notifications" className='cancel' params={{order: notification.order}}>Cancel</Link>
            <button className='btn' type='submit'>Resend</button>
          </div>
        </form>
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
    contactType: 'Email',
    contact: 'jim@bob.com',
    order: 1234
  }
};

export default Notification;
