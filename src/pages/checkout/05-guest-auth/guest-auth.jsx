/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';

import Guest from '../../../components/auth/guest';
import Login from '../../../components/auth/login';

import localized from 'lib/i18n';

import styles from './guest-auth.css';

@localized
class GuestAuth extends Component {

  @autobind
  getPath(newType: ?string): Object {
    return newType ? assoc(this.props.path, ['query', 'auth'], newType) : dissoc(this.props.path, ['query', 'auth']);
  }

  render() {
    if (!this.props.isEditing) {
      return null;
    }

    return (
      <article styleName="guest-auth">
        <div styleName="auth-block">
          <header styleName="header">SIGN IN & CHECKOUT</header>
          <div styleName="form">
            <div styleName="form-content">
              <Login getPath={this.getPath} displayTitle={false} />
            </div>
          </div>
        </div>
        <div styleName="auth-block">
          <header styleName="header">CHECKOUT AS GUEST</header>
          <div styleName="form">
            <div styleName="form-content">
            </div>
          </div>
        </div>
      </article>
    );
  }
}

export default localized(GuestAuth);
