
/* @flow */

import React, { Element } from 'react';

import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';
import wrapModal from '../modal/wrapper';

type Props = {
  body: string|Element,
  header: string|Element,
  cancel: string,
  confirm: string,
  icon?: string,
  cancelAction: Function,
  confirmAction: Function,
};

const ConfirmationDialog = (props: Props): Element => {
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

  const handleKeyPress = (event) => {
    if (event.keyCode === 13 /*enter*/) {
      event.preventDefault();
      props.confirmAction();
    }
  };

  return (
      <div onKeyDown={handleKeyPress}>
        <ContentBox title={title} className="fc-confirmation-dialog" actionBlock={actionBlock}>
          <div className='fc-modal-body'>
            {props.body}
          </div>
          <div className='fc-modal-footer'>
            <a tabIndex="2" className='fc-modal-close' onClick={() => props.cancelAction()}>
              {props.cancel}
            </a>
            <PrimaryButton tabIndex="1" autoFocus={true}
                           onClick={() => props.confirmAction()}>
              {props.confirm}
            </PrimaryButton>
          </div>
        </ContentBox>
      </div>
  );
};

export default wrapModal(ConfirmationDialog);
