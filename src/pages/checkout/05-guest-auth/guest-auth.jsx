/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';
import { connect } from 'react-redux';

import Guest from '../../../components/auth/guest';
import Login from '../../../components/auth/login';

import * as checkoutActions from 'modules/checkout';
import * as authActions from 'modules/auth';

import localized from 'lib/i18n';

import styles from './guest-auth.css';

@localized
@connect(null, {...checkoutActions, ...authActions})
class GuestAuth extends Component {

  @autobind
  onGuestCheckout(email: string) {
    this.props.saveEmail(email).then(() => {
      this.props.continueAction();
    });
  }

  @autobind
  onCheckoutAfterAuth() {
    this.props.checkoutAfterSignIn();
  }

  @autobind
  getPath(newType: ?string): Object {
    const { location } = this.props;
    return newType ? assoc(location, ['query', 'auth'], newType) : dissoc(location, ['query', 'auth']);
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
              <Login
                getPath={this.getPath}
                displayTitle={false}
                onGuestCheckout={this.onCheckoutAfterAuth}
              />
            </div>
          </div>
        </div>
        <div styleName="auth-block">
          <header styleName="header">CHECKOUT AS GUEST</header>
          <div styleName="form">
            <div styleName="form-content">
              <Guest onGuestCheckout={this.onGuestCheckout}/>
            </div>
          </div>
        </div>
      </article>
    );
  }
}

export default localized(GuestAuth);
