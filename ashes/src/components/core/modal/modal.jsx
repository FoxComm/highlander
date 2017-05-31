/* @flow */

// libs
import noop from 'lodash/noop';
import classNames from 'classnames';
import React, { Element } from 'react';

// components
import { ModalContainer } from 'components/core/modal-container';

// styles
import s from './modal.css';

type Props = {
  isVisible: boolean,
  onClose: () => any,
  title: string | Element<any>,
  footer?: Element<any>,
  children?: Element<any>,
  className?: string,
};

export default ({ isVisible, title, footer, children, className, onClose = noop }: Props) => (
  <ModalContainer
    className={classNames(s.modal, className)}
    isVisible={isVisible}
    onClose={onClose}
  >
    <header className={s.header}>
      <div className={s.title}>{title}</div>
      <a className={s.close} onClick={onClose} title="Close">&times;</a>
    </header>
    <div className={s.body}>
      {children}
    </div>
    {footer && <footer className={s.footer}>{footer}</footer>}
  </ModalContainer>
);
