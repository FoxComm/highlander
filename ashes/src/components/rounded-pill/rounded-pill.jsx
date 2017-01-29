/**
 * @flow
 */

import React, { PropTypes } from 'react';

import styles from './rounded-pill.css';

type Props = {
  text: string,
  value?: string,
  onClose?: (value: string) => void,
  className?: string,
  closeBtnId?: string,
};

const RoundedPill = (props: Props) => {
  const { closeBtnId, className, onClose, value, text } = props;

  let closeButton = null;
  if (onClose && value) {
    closeButton = (
      <button id={closeBtnId} styleName="button" onClick={() => onClose(value)}>
        <i className="icon-close" />
      </button>
    );
  }

  const styleName = onClose ? 'main-closable' : 'main';

  return (
    <div styleName={styleName} className={className}>
      <div styleName="text">{text}</div>
      {closeButton}
    </div>
  );
};

export default RoundedPill;
