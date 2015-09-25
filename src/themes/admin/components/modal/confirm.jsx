'use strict';

import React from 'react';
import { camelize } from 'fleck';
import { dispatch } from '../../lib/dispatcher';

export default class ConfirmModal extends React.Component {
  confirmModal() {
    let eventName = camelize(this.props.event);
    dispatch(eventName);
  }

  render() {
    let modalOptions = this.props.details;

    return (
      <div>
        <div className='fc-modal-header'>
          <div className='icon'>
            <i className='icon-warning'></i>
          </div>
          <div className='title'>{modalOptions.header}</div>
          <a className='fc-modal-close' aria-label='close' onClick={dispatch.bind(null, 'toggleModal', null)}>
            <span aria-hidden='true'>&times;</span>
          </a>
        </div>
        <div className='fc-modal-body'>
          {modalOptions.body}
        </div>
        <div className='fc-modal-footer'>
          <a className='fc-modal-close' onClick={dispatch.bind(null, 'toggleModal', null)}>{modalOptions.cancel}</a>
          <button className='submit btn' onClick={this.confirmModal.bind(this)}>{modalOptions.proceed}</button>
        </div>
      </div>
    );
  }
}

ConfirmModal.propTypes = {
  event: React.PropTypes.string,
  details: React.PropTypes.object
};

ConfirmModal.defaultProps = {
  details: {
    header: 'Confirm',
    body: 'Are you sure you wish to proceed?',
    cancel: 'No',
    proceed: 'Yes'
  }
};
