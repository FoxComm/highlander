/**
 * @flow
 */

import classNames from 'classnames';
import React from 'react';

import styles from './rounded-pill.css';

type Props = {
  text: string,
  value?: string,
  onClose?: (value: string) => void,
  className?: string,
  pillId?: string,
  inProgress: boolean,
};

const RoundedPill = (props: Props) => {
  const { pillId, className, onClose, value, text, inProgress } = props;

  let closeButton = null;
  if (onClose && value) {
    closeButton = (
      <button className="fct-pill-close-btn" styleName="button" onClick={() => onClose(value)}>
        <i className="icon-close" />
      </button>
    );
  }

  const cls = classNames(styles.main, {
    [styles.closable]: onClose,
    [styles._loading]: inProgress,
  }, className);

  return (
    <div id={pillId} className={cls}>
      <div className="fct-pill-label" styleName="text">{text}</div>
      {closeButton}
    </div>
  );
};

export default RoundedPill;
