// @flow

import React, { Element } from 'react';
import styles from './popup.css';

type Props = {
  title: string|Element<*>,
  children?: Element<*>,
  onClose: () => void,
}

const Popup = (props: Props) => {
  return (
    <div styleName="block">
      <div styleName="close-button" onClick={props.onClose} />
      <div styleName="title">{props.title}</div>
      <div styleName="content">
        {props.children}
      </div>
    </div>
  );
};

export default Popup;
