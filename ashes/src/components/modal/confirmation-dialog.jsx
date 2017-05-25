/* @flow */

// libs
import _ from 'lodash';
import classNames from 'classnames';
import React, { Element, Component } from 'react';

// components
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import wrapModal from 'components/modal/wrapper';
import ApiErrorAlert from 'components/core/utils/api-errors-alert';

type Props = {
  body: string | Element<*>,
  header: string | Element<*>,
  cancel: string,
  confirm: string,
  onCancel: Function,
  confirmAction: Function,
  icon?: string,
  asyncState?: AsyncState,
  className?: string,
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

  const handleKeyPress = (event) => {
    if (event.keyCode === 13 /*enter*/) {
      event.preventDefault();
      props.confirmAction();
    }
  };

  const cls = classNames('fc-confirmation-dialog', props.className);

  return (
    <div onKeyDown={handleKeyPress}>
      <ContentBox title={title} className={cls} actionBlock={actionBlock}>
        <div className='fc-modal-body'>
          <ApiErrorAlert response={_.get(props.asyncState, 'err', null)} />
          {props.body}
        </div>

        <SaveCancel
          className="fc-modal-footer"
          onCancel={props.onCancel}
          onSave={props.confirmAction}
          saveText={props.confirm}
          isLoading={_.get(props.asyncState, 'inProgress', false)}
        />
      </ContentBox>
    </div>
  );
};

const Wrapped: Class<Component<void, Props, any>> = wrapModal(ConfirmationDialog);

export default Wrapped;
