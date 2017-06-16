/* @flow */

import React from 'react';
import styles from './content.css';

type Props = {
  children?: any,
};

const Content = (props: Props) => {
  return (
    <div styleName="body">
      {props.children}
    </div>
  );
};

export default Content;
