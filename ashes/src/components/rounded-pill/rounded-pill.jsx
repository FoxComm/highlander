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
  pillId?: string,
};

const RoundedPill = (props: Props) => {
  const { pillId, className, onClose, value, text } = props;

  let closeButton = null;
  if (onClose && value) {
    closeButton = (
      <button className="fct-pill-close-btn" styleName="button" onClick={() => onClose(value)}>
        <i className="icon-close" />
      </button>
    );
  }

  const styleName = onClose ? 'main-closable' : 'main';

  return (
    <div id={pillId} styleName={styleName} className={className}>
      <div className="fct-pill-label" styleName="text">{text}</div>
      {closeButton}
    </div>
  );
};

export default RoundedPill;
