'use strict';

import React from 'react';
import { dispatch } from '../../lib/dispatcher';
import NotificationStore from './store';

export default class ResendModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      edit: false
    };
  }

  toggleEdit() {
    this.setState({edit: !this.state.edit});
  }

  resendNotification(e) {
    e.preventDefault();

    let notification = this.props.notification;
    NotificationStore.resend(notification.id);

    dispatch('toggleModal', null);
  }

  render() {
    let notification = this.props.notification;

    let innerContent = <strong>{notification.contact}</strong>;
    if (this.state.edit) {
      innerContent = <input type={notification.contactType === 'Email' ? 'email' : 'tel'} className='control' name='resend_to' />;
    }

    return (
      <form method='POST' id='resend' onSubmit={this.resendNotification.bind(this)}>
        <div className='modal-header'>
          <div className='icon'>
            <i className='icon-attention'></i>
          </div>
          <div className='title'>Resend Message?</div>
          <a className='close' aria-label='Close' onClick={dispatch.bind(null, 'toggleModal', null)}><span aria-hidden="true">&times;</span></a>
        </div>
        <div className='modal-body'>
          You will send another copy of this message to:
          <div className='contact'>
            {innerContent}
            <a onClick={this.toggleEdit.bind(this)}>{this.state.edit ? 'Undo' : 'Edit'}</a>
          </div>
        </div>
        <div className='modal-footer'>
          <a className='close' onClick={dispatch.bind(null, 'toggleModal', null)}>Cancel</a>
          <input type='submit' value='Resend' className='btn' />
        </div>
      </form>
    );
  }
}

ResendModal.propTypes = {
  notification: React.PropTypes.object
};
