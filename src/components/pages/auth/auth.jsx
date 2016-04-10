/* @flow */
import React, { Component } from 'react';
import { connect } from 'react-redux';

import { authBlockToggle } from 'modules/auth';

import styles from './auth.css';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';

type AuthProps = {
  children: HTMLElement;
  authBlockToggle: Function;
}

const Auth = (props: AuthProps): HTMLElement => {
  return (
    <div styleName="auth-block">
        <Icon styleName="logo" name="fc-some_brand_logo" />
        {props.children}
        <a styleName="close-button" onClick={props.authBlockToggle}>
            <Icon name="fc-close" className="close-icon"/>
        </a>
    </div>
  );
};

const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible
});

export default connect(mapState, { authBlockToggle }) (Auth);