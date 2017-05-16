/* @flow */

import React, { Element } from 'react';

import styles from './layout.css';

type Props = {
  childrend: Element<*>,
};

const Layout = (props: Props) => {
  return (
    <div styleName="container">
      <div styleName="header">
      </div>

      {props.children}
    </div>
  );
};

export default Layout;
