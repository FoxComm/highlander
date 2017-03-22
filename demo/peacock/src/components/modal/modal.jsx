/* @flow */
import React from 'react';

import Overlay from 'ui/overlay/overlay';

import styles from './modal.css';

type Props = {
  addressesVisible: boolean,
  toggleAddresses: Function,
};

const Modal = (props: Props) => {
  return(
    <div styleName={props.addressesVisible ? 'modal-show' : 'modal'}>
      <Overlay onClick={props.toggleAddresses} shown={props.addressesVisible} />
      <div styleName='modal-wrapper'>
        {props.children}
      </div>
    </div>
  );
};

export default Modal;
