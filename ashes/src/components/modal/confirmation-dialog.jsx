/* @flow */

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';
import wrapModal from 'components/modal/wrapper';
import ErrorAlerts from 'components/alerts/error-alerts';

type Props = {
  body: string|Element<*>,
  header: string|Element<*>,
  cancel: string,
  confirm: string,
  icon?: string,
  onCancel: Function,
  confirmAction: Function,
  asyncState?: AsyncState,
};

const ConfirmationDialog = (props: Props) => {
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

        <SaveCancel
          className="fc-modal-footer"
          onCancel={props.onCancel}
          onSave={props.confirmAction}
          saveText={props.confirm}
        />
      </ContentBox>
    </div>
  );
};

export default wrapModal(ConfirmationDialog);
