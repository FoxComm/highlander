/* @flow */

import React from 'react';
import styles from './auth.css';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';

type AuthProps = {
  children: HTMLElement;
}

const Auth = (props: AuthProps) => {
  return (
    <div styleName="auth-block">
      <Icon styleName="logo" name="fc-some_brand_logo" />
      {props.children}
    </div>
  );
};

export default Auth;
