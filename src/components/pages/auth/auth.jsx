/* @flow */
import React, { Component } from 'react';
import { connect } from 'react-redux';

import { authBlockToggle, authBlockTypes } from 'modules/auth';

import styles from './auth.css';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';
import Login from './login';
import Signup from './signup';
import ResetPassword from './reset-password.jsx';
import RestorePassword from './restore-password.jsx';

class Auth extends Component {
  renderContent() {
    switch (this.props.authBlockType) {
      case authBlockTypes.LOGIN:
        return <Login />;
      case authBlockTypes.SIGNUP:
        return <Signup />;
      case authBlockTypes.RESET_PASSWORD:
        return <ResetPassword />;
      case authBlockTypes.RESTORE_PASSWORD:
        return <RestorePassword />;
      default:
        return <Login />;
    }
  }

  render(): HTMLElement {
    return (
      <div styleName="auth-block">
        <Icon styleName="logo" name="fc-some_brand_logo"/>
        {this.props.isAuthBlockVisible ? this.renderContent() : this.props.children}
        <a styleName="close-button" onClick={this.props.authBlockToggle}>
          <Icon name="fc-close" className="close-icon"/>
        </a>
      </div>
    );
  }
}

const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible,
  authBlockType: state.auth.authBlockType,
});

export default connect(mapState, { authBlockToggle })(Auth);
