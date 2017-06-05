/* @flow */

// libs
import noop from 'lodash/noop';
import classNames from 'classnames';
import React, { Element } from 'react';

// components
import ModalContainer from 'components/core/modal-container';

// styles
import s from './modal.css';

type Props = {
  /** If modal is active or not */
  isVisible: boolean,
  /** Callback to handle close events (overlay/esc click) */
  onClose: () => any,
  /** Header string */
  title: string | Element<any>,
  /** Footer content */
  footer?: Element<any>,
  /** Modal content */
  children?: Element<any>,
  /** Additional className */
  className?: string,
};

/**
 * Modal component represents project-wide styled modal with title, close button and optional footer.
 * It should be used for all common modal windows.
 *
 * If you need custom-styled modal use `components/core/modal-container` instead.
 *
 * @function Modal
 */
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
