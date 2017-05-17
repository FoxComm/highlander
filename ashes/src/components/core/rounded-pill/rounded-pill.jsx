// @flow

import noop from 'lodash/noop';
import classNames from 'classnames';
import React, { Element } from 'react';

import s from './rounded-pill.css';

export type Value = string|number;

type Props = {
  text: string,
  value?: Value,
  onClose?: (value: Value) => any,
  onClick?: (value: Value) => any,
  className?: string,
  inProgress?: boolean,
  pillId?: string,
};

export const RoundedPill = (props: Props) => {
  const { className, onClick, onClose, value, text, inProgress, pillId } = props;

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

  const handleClick = onClick && value ? () => onClick(value) : noop;

  return (
    <div className={cls} key={value} id={pillId}>
      <div className={s.label} onClick={handleClick}>{text}</div>
      {closeButton}
    </div>
  );
};
