'use strict';

import React from 'react';
import { camelize } from 'fleck';
import { dispatch } from '../../lib/dispatcher';

// THIS COMPONENT IS DEPRECATED.
// When doing new development, please use ConfirmationDialog instead.
export default class ConfirmModal extends React.Component {
  confirmModal() {
    if (this.props.closeOnSuccess) {
      dispatch('toggleModal', null);
    }

    if (this.props.callback) {
      this.props.callback(true);
    }
  }

  render() {
    let modalOptions = this.props.details;

    return (
      <div className="fc-modal-confirm">
        <div className='fc-modal-header'>
          <div className='fc-modal-icon'>
            <i className='icon-warning'></i>
          </div>
          <div className='fc-modal-title'>{modalOptions.header}</div>
          <a className='fc-modal-close' aria-label='close' onClick={dispatch.bind(null, 'toggleModal', null)}>
            <span aria-hidden='true'>&times;</span>
          </a>
        </div>
        <div className='fc-modal-body'>
          {modalOptions.body}
        </div>
        <div className='fc-modal-footer'>
          <a className='fc-modal-close' onClick={dispatch.bind(null, 'toggleModal', null)}>{modalOptions.cancel}</a>
          <button className='fc-btn' onClick={this.confirmModal.bind(this)}>{modalOptions.proceed}</button>
        </div>
      </div>
    );
  }
}

ConfirmModal.propTypes = {
  details: React.PropTypes.object,
  callback: React.PropTypes.func,
  closeOnSuccess: React.PropTypes.bool
};

ConfirmModal.defaultProps = {
  details: {
    header: 'Confirm',
    body: 'Are you sure you wish to proceed?',
    cancel: 'No',
    proceed: 'Yes'
  },
  closeOnSuccess: true
};
