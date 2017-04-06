/**
 * @flow
 */

import classNames from 'classnames';
import React from 'react';

import s from './rounded-pill.css';

type Props = {
  text: string,
  value?: string | number,
  onClose?: (value: string) => any,
  onClick?: (value: string) => any,
  className?: string,
  inProgress?: boolean,
};

const RoundedPill = (props: Props) => {
  const { className, onClick, onClose, value, text, inProgress } = props;

  let closeButton = null;
  if (onClose && value) {
    closeButton = (
      <button className={s.button} onClick={() => onClose(value)}>&times;</button>
    );
  }

  const cls = classNames(s.main, {
    [s.clickable]: onClick,
    [s.closable]: onClose,
    [s._loading]: inProgress,
  }, className);

  return (
    <div className={cls} key={value}>
      <div className={s.label} onClick={() => onClick(value)}>{text}</div>
      {closeButton}
    </div>
  );
};

export default RoundedPill;
