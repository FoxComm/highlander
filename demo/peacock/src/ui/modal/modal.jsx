/* @flow */

import React from 'react';
import classNames from 'classnames';

import Overlay from 'ui/overlay/overlay';

import styles from './modal.css';

type Props = {
  show: boolean,
  toggle: Function,
  children?: any,
};

const Modal = (props: Props) => {
  const modalClass = classNames(styles.modal, {
    [styles.show]: props.show,
  });

  return (
    <div className={modalClass}>
      <Overlay onClick={props.toggle} shown={props.show} />
      <div styleName="modal-wrapper">
        {props.children}
      </div>
    </div>
  );
};

export default Modal;
