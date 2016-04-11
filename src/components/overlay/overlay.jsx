/* @flow */

import React from 'react';
import styles from './overlay.css';
import type { HTMLElement } from 'types';

type OverlayProps = {
  children: HTMLElement;
}

const Overlay = (props:OverlayProps) => {
  return (
    <div styleName="overlay">
      {props.children}
    </div>
  );
};

export default Overlay;
