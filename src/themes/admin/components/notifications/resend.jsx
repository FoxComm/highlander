'use strict';

import React from 'react';
import ContactMethod from './method';
import { dispatch } from '../../lib/dispatcher';

class ResendModal extends React.Component {
  render() {
    let notification = this.props.notification;

    return (
      <form action="POST">
        <div className='modal-header'>
          <div className='icon'>
            <i className='icon-attention'></i>
          </div>
          <div className='title'>Resend Message?</div>
          <a className='close' aria-label='Close' onClick={dispatch.bind(null, 'closeModal')}><span aria-hidden="true">&times;</span></a>
        </div>
        <div className='modal-body'>
          You will send another copy of this message to:
          <ContactMethod type={notification.contactType} value={notification.contact} />
        </div>
        <div className='modal-footer'>
          <a className='close' onClick={dispatch.bind(null, 'closeModal')}>Cancel</a>
          <button className='btn' type='submit'>Resend</button>
        </div>
      </form>
    );
  }
}

ResendModal.propTypes = {
  notification: React.PropTypes.object
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
