/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { browserHistory, Link } from 'react-router';
import { connect } from 'react-redux';

import styles from './auth.css';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Button from 'ui/buttons';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'paragons/auth';
import { fetch as fetchCart, saveLineItems } from 'modules/cart';

import type { HTMLElement } from 'types';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

type AuthState = {
  email: string,
  password: string,
  error: ?string,
};

type Props = Localized & {
  getPath: Function,
  isLoading: boolean,
  authenticate: Function,
  fetchCart: Function,
  saveLineItems: Function,
  onGuestCheckout?: Function,
  displayTitle: boolean,
};

const mapState = state => ({
  cart: state.cart,
  isLoading: _.get(state.asyncActions, ['auth-login', 'inProgress'], false),
});

class Login extends Component {
  props: Props;

  state: AuthState = {
    email: '',
    password: '',
    error: null,
  };

  static defaultProps = {
    displayTitle: true,
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
      error: null,
    });
  }

  @autobind
  onChangePassword({target}: any) {
    this.setState({
      password: target.value,
      error: null,
    });
  }

  @autobind
  authenticate(e: any) {
    e.preventDefault();
    e.stopPropagation();
    const { email, password } = this.state;
    const kind = 'merchant';
    const auth = this.props.authenticate({email, password, kind}).then(() => {
      const lineItems = _.get(this.props, 'cart.lineItems', []);
      if (_.isEmpty(lineItems)) {
        this.props.fetchCart();
      } else {
        this.props.saveLineItems();
      }
      browserHistory.push(this.props.getPath());
    }, () => {
      this.setState({error: 'Email or password is invalid'});
    });

    if (this.props.onGuestCheckout != null) {
      auth.then(() => {
        this.props.onGuestCheckout();
      });
    }
  }

  @autobind
  googleAuthenticate(e: any) {
    e.preventDefault();
    e.stopPropagation();
    this.props.googleSignin().then(() => {
      this.props.fetchCart();
    });
  }

  get title() {
    const { t } = this.props;
    return this.props.displayTitle
      ? <div styleName="title">{t('SIGN IN')}</div>
      : null;
  }

  render(): HTMLElement {
    const { password, email } = this.state;
    const { t, getPath } = this.props;

    const restoreLink = (
      <Link to={getPath(authBlockTypes.RESTORE_PASSWORD)} styleName="restore-link">
        {t('forgot?')}
      </Link>
    );

    const signupLink = (
      <Link to={getPath(authBlockTypes.SIGNUP)} styleName="link">
        {t('Sign Up')}
      </Link>
    );

    return (
      <div>
        {this.title}
        <form>
          <FormField key="email" styleName="form-field" error={this.state.error}>
            <TextInput placeholder={t('EMAIL')} value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field" error={!!this.state.error}>
            <TextInputWithLabel
              styleName="form-field-input"
              placeholder="PASSWORD"
              label={!password && restoreLink}
              value={password}
              onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button
            styleName="primary-button"
            isLoading={this.props.isLoading}
            onClick={this.authenticate}
          >
            {t('SIGN IN')}
          </Button>
        </form>
        <div styleName="switch-stage">
          {t('Don’t have an account?')} {signupLink}
        </div>
      </div>
    );
  }
}

export default connect(mapState, {
  ...actions,
  fetchCart,
  saveLineItems,
})(localized(Login));
