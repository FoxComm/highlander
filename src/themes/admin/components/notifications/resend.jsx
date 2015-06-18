'use strict';

import React from 'react';
import { dispatch } from '../../lib/dispatcher';

class ResendModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      edit: false
    };
  }

  toggleEdit() {
    this.setState({edit: !this.state.edit});
  }

  render() {
    let notification = this.props.notification;
    let order = notification.order;

    let innerContent = <strong>{notification.contact}</strong>;
    if (this.state.edit) {
      innerContent = <input type={notification.contactType === 'Email' ? 'email' : 'tel'} className='control' name='resend_to' />;
    }

    let url = `/order/${order.id}/notifications/${notification.id}`;

    return (
      <form method="POST" action={url}>
        <div className='modal-header'>
          <div className='icon'>
            <i className='icon-attention'></i>
          </div>
          <div className='title'>Resend Message?</div>
          <a className='close' aria-label='Close' onClick={dispatch.bind(null, 'closeModal')}><span aria-hidden="true">&times;</span></a>
        </div>
        <div className='modal-body'>
          You will send another copy of this message to:
          <div className='contact'>
            {innerContent}
            <a onClick={this.toggleEdit.bind(this)}>{this.state.edit ? 'Undo' : 'Edit'}</a>
          </div>
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
