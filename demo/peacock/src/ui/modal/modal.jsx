/* @flow */
import React from 'react';
import classNames from 'classnames';

import Overlay from 'ui/overlay/overlay';

import styles from './modal.css';

type Props = {
  addressesVisible: boolean,
  toggleAddresses: Function,
};

const Modal = (props: Props) => {
  const modalClass = classNames(styles['modal'], {
    [styles['show']]: props.addressesVisible,
  });

  return(
    <div className={modalClass}>
      <Overlay onClick={props.toggleAddresses} shown={props.addressesVisible} />
      <div styleName='modal-wrapper'>
        {props.children}
      </div>
    </div>
  );
};

export default Modal;
