/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import Copyright from '../../../components/footer/copyright';
import Guest from '../../../components/auth/guest';
import Login from '../../../components/auth/login';
import Signup from '../../../components/auth/signup';
import Icon from 'ui/icon';

import * as checkoutActions from 'modules/checkout';
import * as authActions from 'modules/auth';

import localized from 'lib/i18n';

import styles from './guest-auth.css';

type State = {
  authMode: string,
}

@localized
@connect(null, {...checkoutActions, ...authActions})
class GuestAuth extends Component {
  state: State = {
    authMode: 'login',
  };

  @autobind
  onGuestCheckout(email: string) {
    this.props.saveEmail(email).then(() => {
      this.props.continueAction();
    });
  }

  @autobind
  getPath(newType: ?string): Object {
    const { location } = this.props;
    return newType ? assoc(location, ['query', 'auth'], newType) : dissoc(location, ['query', 'auth']);
  }

  @autobind
  toggleAuthForm(event) {
    event.preventDefault();
    event.stopPropagation();

    this.setState({
      authMode: this.state.authMode === 'login' ? 'signup' : 'login',
    });
  }

  get authForm() {
    if (this.state.authMode == 'login') {
      return (
        <Login
          mergeGuestCart
          getPath={this.getPath}
          title="LOG IN & CHECKOUT"
          onSignupClick={this.toggleAuthForm}
        />
      );
    }
    return (
      <Signup
        mergeGuestCart
        getPath={this.getPath}
        title="SIGN UP & CHECKOUT"
        onLoginClick={this.toggleAuthForm}
      />
    );
  }

  render() {
    if (!this.props.isEditing) {
      return null;
    }

    return (
      <article styleName="guest-auth">
        <div styleName="home">
          <Link to="/">
             <Icon styleName="logo" name="fc-logo" />
          </Link>
          <div styleName="divider" />
          <p styleName="title">Checkout</p>
          <div styleName="divider" />
        </div>
        <div styleName="forms">
          <div styleName="auth-block">
            {this.authForm}
          </div>
          <div styleName="divider" />
          <div styleName="mobile-divider-block">
            <div styleName="mobile-divider" />
            <p>or</p>
            <div styleName="mobile-divider" />
          </div>
          <div styleName="auth-block">
            <Guest onGuestCheckout={this.onGuestCheckout}/>
          </div>
        </div>
        <div styleName="footer">
          <Copyright styleName="copyright" />
        </div>
      </article>
    );
  }
}

export default localized(GuestAuth);
