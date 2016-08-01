
// @flow

import React, { Element } from 'react';

import styles from './auth-pages.css';

type Props = {
  children: Element,
};

const AuthPages = (props: Props) => {
  return (
    <div styleName="body">
      <img styleName="logo" src="/images/fc-logo-v.svg"/>
      {props.children}
      <div styleName="copyright">
        © 2016 FoxCommerce. All rights reserved. Privacy Policy. Terms of Use.
      </div>
    </div>
  );
};

export default AuthPages;
