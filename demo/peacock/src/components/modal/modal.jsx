/* @flow */
import React from 'react';

import Overlay from 'ui/overlay/overlay';

import styles from './modal.css';

type Props = {
  addressesVisible: boolean,
  toggleAddresses: Function,
  title: string,
};

const Modal = (props: Props) => {
  return(
    <div styleName={props.addressesVisible ? 'modal-show' : 'modal'}>
      <Overlay onClick={props.toggleAddresses} shown={props.addressesVisible} />
      <div styleName='modal-wrapper'>
        <header styleName="modal-header">
          {props.title}
          {props.action}
        </header>
        <div styleName="modal-body">
          {props.children}
        </div>
        <div styleName="modal-footer">
          {props.footer}
        </div>
      </div>
    </div>
  );
};

export default Modal;
