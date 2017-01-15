
/* @flow */

import _ from 'lodash';
import React, { Element } from 'react';

import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';
import wrapModal from '../modal/wrapper';
import ErrorAlerts from '../alerts/error-alerts';

type Props = {
  body: string|Element,
  header: string|Element,
  cancel: string,
  confirm: string,
  icon?: string,
  onCancel: Function,
  confirmAction: Function,
  asyncState?: AsyncState,
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
    <a className='fc-modal-close' onClick={() => props.onCancel()}>
      <i className='icon-close' />
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
          <ErrorAlerts error={_.get(props.asyncState, 'err', null)} />
          {props.body}
        </div>
        <div className='fc-modal-footer'>
          <a id="modal-cancel-btn" tabIndex="2" className='fc-modal-close' onClick={() => props.onCancel()}>
            {props.cancel}
          </a>
          <PrimaryButton id="modal-confirm-btn"
                         tabIndex="1" autoFocus={true}
                         isLoading={_.get(props.asyncState, 'inProgress', false)}
                         onClick={() => props.confirmAction()}>
            {props.confirm}
          </PrimaryButton>
        </div>
      </ContentBox>
    </div>
  );
};

export default wrapModal(ConfirmationDialog);
