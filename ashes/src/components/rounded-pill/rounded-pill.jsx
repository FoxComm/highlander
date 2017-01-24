/**
 * @flow
 */

import React, { PropTypes } from 'react';

import styles from './rounded-pill.css';

type Props = {
  text: string,
  value: string,
  onClose: (value: string) => void,
  className?: string,
  closeBtnId: string,
};

const RoundedPill = (props: Props) => {
  let closeButton = null;
  if (props.onClose) {
    closeButton = (
      <button id={props.closeBtnId} styleName="button" onClick={() => props.onClose(props.value)}>
        <i className="icon-close" />
      </button>
    );
  }

  const styleName = props.onClose ? 'main-closable' : 'main';

  return (
    <div styleName={styleName} className={props.className}>
      <div styleName="text">{props.text}</div>
      {closeButton}
    </div>
  );
};

export default RoundedPill;
