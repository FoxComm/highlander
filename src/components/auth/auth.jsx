/* @flow  */

import React, { Component } from 'react';
import { Link } from 'react-router';
import { authBlockTypes } from 'paragons/auth';
import { assoc, dissoc } from 'sprout-data';
import { autobind } from 'core-decorators';

import styles from './auth.css';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';
import Login from './login';
import Signup from './signup';
import ResetPassword from './reset-password.jsx';
import RestorePassword from './restore-password.jsx';

type Props = {
  authBlockType: string,
  path: Object,
};

class Auth extends Component {
  props: Props;

  renderContent() {
    const authProps = {
      getPath: this.getPath,
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

  @autobind
  getPath(newType: ?string): Object {
    return newType ? assoc(this.props.path, ['query', 'auth'], newType) : dissoc(this.props.path, ['query', 'auth']);
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
