'use strict';

import React from 'react';
import ContactMethod from './method';

class ResendModal extends React.Component {
  render() {
    let notification = this.props.notification;

    return (
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
          <a className='cancel' onClick={this.props.closeHandler}>Cancel</a>
          <button className='btn' type='submit'>Resend</button>
        </div>
      </form>
    );
  }
}

ResendModal.propTypes = {
  notification: React.PropTypes.object,
  closeHandler: React.PropTypes.func,
  openHandler: React.PropTypes.func
};

ResendModal.defaultProps = {
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

export default ResendModal;
