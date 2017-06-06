/* @flow */

import React from 'react';
import classNames from 'classnames';

import s from './overlay.css';

type Props = {
  shown: bool,
  onClick?: Function,
};

const Overlay = (props: Props) => {
  const cls = classNames(s.overlay, {
    [s.active]: props.shown,
  });

  return <div className={cls} onClick={props.onClick} />;
};

export default Overlay;
