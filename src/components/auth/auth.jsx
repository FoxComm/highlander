/* @flow */
import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import { authBlockTypes } from 'modules/auth';

import styles from './auth.css';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';
import Login from './login';
import Signup from './signup';
import ResetPassword from './reset-password.jsx';
import RestorePassword from './restore-password.jsx';

class Auth extends Component {
  static propTypes = {
    authBlockType: PropTypes.string,
    path: PropTypes.string,
  };

  renderContent() {
    const authProps = {
      path: this.props.path,
    };

    switch (this.props.authBlockType) {
      case authBlockTypes.LOGIN:
        return <Login {...authProps} />;
      case authBlockTypes.SIGNUP:
        return <Signup {...authProps} />;
      case authBlockTypes.RESET_PASSWORD:
        return <ResetPassword {...authProps} />;
      case authBlockTypes.RESTORE_PASSWORD:
        return <RestorePassword {...authProps} />;
      default:
        return <Login {...authProps} />;
    }
  }

  render(): HTMLElement {
    return (
      <div styleName="auth-block">
        <Link to="/">
          <Icon styleName="logo" name="fc-some_brand_logo" />
        </Link>
        {this.renderContent()}
      </div>
    );
  }
}

export default Auth;
