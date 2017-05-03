
/* @flow */

import React from 'react';
import classNames from 'classnames';

import styles from './overlay.css';

type Props = {
  shown: bool,
  onClick?: Function,
};

const Overlay = (props: Props) => {
  const style = classNames({
    'overlay_hidden': !props.shown,
    'overlay_shown': props.shown,
  });

  return <div styleName={style} onClick={props.onClick} />;
};

export default Overlay;
