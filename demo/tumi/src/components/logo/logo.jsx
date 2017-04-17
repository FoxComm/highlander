// @flow

import React from 'react';
import styles from './logo.css';

type Props = {
  className?: string,
}

const Logo = (props: Props) => {
  return (
    <div styleName="logo" className={props.className}>TUMI</div>
  );
};

export default Logo;
