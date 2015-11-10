
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { ModalContainer } from './base';

const ConfirmationDialog = props => {
  return (
    <ModalContainer {...props}>
      <div className='fc-modal-confirm'>
        <div className='fc-modal-header'>
          <div className='fc-modal-icon'>
            <i className='icon-warning'></i>
          </div>
          <div className='fc-modal-title'>{props.header}</div>
          <a className='fc-modal-close' onClick={() => props.cancelAction()}>
            <span>&times;</span>
          </a>
        </div>
        <div className='fc-modal-body'>
          {props.body}
        </div>
        <div className='fc-modal-footer'>
          <a tabIndex="2" className='fc-modal-close' onClick={() => props.cancelAction()}>
            {props.cancel}
          </a>
          <button tabIndex="1" className='fc-btn' autoFocus={true}
                  onClick={() => props.confirmAction()}
                  onKeyUp={({keyCode}) => keyCode === 27 && props.cancelAction()}
                  >
            {props.confirm}
          </button>
        </div>
      </div>
    </ModalContainer>
  );
};

ConfirmationDialog.propTypes = {
  body: PropTypes.node.isRequired,
  cancel: PropTypes.string.isRequired,
  confirm: PropTypes.string.isRequired,
  cancelAction: PropTypes.func.isRequired,
  confirmAction: PropTypes.func.isRequired
};

export default ConfirmationDialog;
