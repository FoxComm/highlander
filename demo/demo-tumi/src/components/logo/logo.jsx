// @flow

import classNames from 'classnames';
import React from 'react';

import styles from './logo.css';

type Props = {
  className?: string,
  onClick?: () => any,
};

const svg =
  `<svg>
      <!-- SAFARI TAB NAVIGATION FIX -->
      <use xlink:href="#fc-logo-icon" />
      <!-- SAFARI TAB NAVIGATION FIX -->
    </svg>`;

const Logo = (props: Props) => {
  return (
    <span
      className={classNames(styles.logo, props.className)}
      onClick={props.onClick}
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
};

export default Logo;
