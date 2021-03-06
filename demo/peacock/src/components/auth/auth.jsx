/* @flow  */

import React, { Component, Element } from 'react';
import { Link } from 'react-router';
import { authBlockTypes } from 'paragons/auth';
import { assoc, dissoc } from 'sprout-data';
import { autobind } from 'core-decorators';

import styles from './auth.css';

import Logo from '../logo/logo';
import Login from './login';
import Signup from './signup';
import ResetPassword from './reset-password';
import RestorePassword from './restore-password';
import ForceRestorePassword from './force-restore-password';

type Props = {
  authBlockType: string,
  path: Object,
};

class Auth extends Component {
  props: Props;

  get body() {
    const authProps = {
      getPath: this.getPath,
      path: this.props.path,
      mergeGuestCart: true,
    };

    switch (this.props.authBlockType) {
      case authBlockTypes.LOGIN:
        return <Login {...authProps} />;
      case authBlockTypes.SIGNUP:
        return <Signup {...authProps} />;
      case authBlockTypes.RESET_PASSWORD:
        return <ResetPassword {...authProps} />;
      case authBlockTypes.FORCE_RESTORE_PASSWORD:
        return <ForceRestorePassword {...authProps} />;
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

  render(): Element<*> {
    return (
      <div styleName="auth-block">
        <Link to={this.props.path} styleName="logo">
          <Logo />
        </Link>
        {this.body}
      </div>
    );
  }
}

export default Auth;
