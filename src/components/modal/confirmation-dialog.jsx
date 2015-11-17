
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { ModalContainer } from './base';
import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';

const ConfirmationDialog = props => {
  let modalIcon = null;
  if (props.icon) {
    modalIcon = <i className='icon-{props.icon}' />;
  }

  let children = (
    <div className='fc-modal-confirm'>
    <div className='fc-modal-header'>
      <div className='fc-modal-icon'>
        {modalIcon}
      </div>
      <div className='fc-modal-title'>{props.header}</div>
      <a className='fc-modal-close' onClick={() => props.cancelAction()}>
        <i className='icon-close'></i>
      </a>
    </div>

  </div>
  );

  let testBlock = (
    <a className='fc-modal-close' onClick={() => props.cancelAction()}>
      <i className='icon-close'></i>
    </a>
  );

  let footer = (
    <div>
      <a tabIndex="2" className='fc-modal-close' onClick={() => props.cancelAction()}>
        {props.cancel}
      </a>
      <PrimaryButton tabIndex="1" autoFocus={true}
                     onClick={() => props.confirmAction()}
                     onKeyUp={({keyCode}) => keyCode === 27 && props.cancelAction()}
      >
        {props.confirm}
      </PrimaryButton>
    </div>
  );

  return (
    <ModalContainer {...props}>
      <ContentBox title={props.header} actionBlock={testBlock}>
        <div className='fc-modal-body'>
          {props.body}
        </div>
        <div className='fc-modal-footer'>
          <a tabIndex="2" className='fc-modal-close' onClick={() => props.cancelAction()}>
            {props.cancel}
          </a>
          <PrimaryButton tabIndex="1" autoFocus={true}
                         onClick={() => props.confirmAction()}
                         onKeyUp={({keyCode}) => keyCode === 27 && props.cancelAction()}>
            {props.confirm}
          </PrimaryButton>
        </div>
      </ContentBox>
    </ModalContainer>
  );
};

ConfirmationDialog.propTypes = {
  body: PropTypes.node.isRequired,
  cancel: PropTypes.string.isRequired,
  confirm: PropTypes.string.isRequired,
  icon: PropTypes.string,
  cancelAction: PropTypes.func.isRequired,
  confirmAction: PropTypes.func.isRequired
};

export default ConfirmationDialog;
