
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { ModalContainer } from './base';
import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';

const ConfirmationDialog = props => {
  let modalIcon = null;
  if (props.icon) {
    modalIcon = <i className={ `icon-${props.icon}` } />;
  }

  const title = (
    <div>
      <div className='fc-modal-icon'>
        {modalIcon}
      </div>
      <div className='fc-modal-title'>{props.header}</div>
    </div>
  );

  const actionBlock = (
    <a className='fc-modal-close' onClick={() => props.cancelAction()}>
      <i className='icon-close'></i>
    </a>
  );

  return (
    <ModalContainer {...props}>
      <ContentBox title={title} className="fc-confirmation-dialog" actionBlock={actionBlock}>
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
  header: PropTypes.node.isRequired,
  cancel: PropTypes.string.isRequired,
  confirm: PropTypes.string.isRequired,
  icon: PropTypes.string,
  cancelAction: PropTypes.func.isRequired,
  confirmAction: PropTypes.func.isRequired
};

export default ConfirmationDialog;
