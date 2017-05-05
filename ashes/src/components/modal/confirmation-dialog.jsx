/* @flow */

// libs
import _ from 'lodash';
import classNames from 'classnames';
import React, { Element, Component } from 'react';

// components
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import wrapModal from 'components/modal/wrapper';
import ErrorAlerts from 'components/alerts/error-alerts';

type Props = {
  body: string|Element<*>,
  header: string|Element<*>,
  cancel: string,
  confirm: string,
  onCancel: Function,
  confirmAction: Function,
  inProgress?: boolean,
  icon?: string,
  asyncState?: AsyncState,
  className?: string,
  focusAction?: boolean,
  focusCancel?: boolean,
};

const ConfirmationDialog = (props: Props) => {
  let modalIcon = null;
  if (props.icon) {
    modalIcon = (
      <div className='fc-modal-icon'>
        <i className={ `icon-${props.icon}` } />
      </div>
    );
  }

  const title = (
    <div>
      {modalIcon}
      <div className='fc-modal-title'>{props.header}</div>
    </div>
  );

  const actionBlock = (
    <a className='fc-modal-close' onClick={() => props.onCancel()}>
      <i className='icon-close' />
    </a>
  );

  const cls = classNames('fc-confirmation-dialog', props.className);

  const inProgress = props.inProgress || _.get(props.asyncState, 'inProgress', false);

  return (
    <ContentBox title={title} className={cls} actionBlock={actionBlock}>
      <div className='fc-modal-body'>
        <ErrorAlerts error={_.get(props.asyncState, 'err', null)} />
        {props.body}
      </div>

      <SaveCancel
        className="fc-modal-footer"
        onCancel={props.onCancel}
        onSave={props.confirmAction}
        saveText={props.confirm}
        isLoading={inProgress}
        cancelDisabled={inProgress}
        saveDisabled={inProgress}
        focusAction={props.focusAction}
        focusCancel={props.focusCancel}
      />
    </ContentBox>
  );
};

const Wrapped: Class<Component<void, Props, any>> = wrapModal(ConfirmationDialog);

export default Wrapped;
