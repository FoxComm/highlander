/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { Link } from 'react-router';
import { connect } from 'react-redux';

import { browserHistory } from 'lib/history';

import styles from './auth.css';

import TextInput from 'ui/text-input/text-input';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'paragons/auth';
import { fetch as fetchCart, saveLineItemsAndCoupons } from 'modules/cart';

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
  saveLineItemsAndCoupons: Function,
  onAuthenticated?: Function,
  title?: string|Element<*>|null,
  onSignupClick: Function,
  mergeGuestCart: boolean,
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
    mergeGuestCart: false,
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
  authenticate() {
    const { email, password } = this.state;
    const kind = 'merchant';
    const auth = this.props.authenticate({email, password, kind}).then(() => {
      this.props.saveLineItemsAndCoupons(this.props.mergeGuestCart);
      browserHistory.push(this.props.getPath());
    }, (err) => {
      const errors = _.get(err, ['responseJson', 'errors'], [err.toString()]);

      const migratedErrorPresent = _.find(errors, (error) => {
        return error.indexOf('is migrated and has to reset password') >= 0;
      });

      if (migratedErrorPresent) {
        browserHistory.push(this.props.getPath(authBlockTypes.FORCE_RESTORE_PASSWORD));
        return;
      }

      this.setState({error: 'Email or password is invalid'});
    });

    if (this.props.onAuthenticated) {
      auth.then(this.props.onAuthenticated);
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
    const { t, title } = this.props;
    return title !== null
      ? <div styleName="title">{title || t('Log in')}</div>
      : null;
  }

  get bottomMessage() {
    const { props } = this;
    const { t } = props;

    return (
      <div styleName="bottom-message">
        <Link to={props.getPath(authBlockTypes.SIGNUP)} onClick={props.onSignupClick} styleName="link">
          {t('Don’t have an account?')}
        </Link>
      </div>
    );
  }

  render(): Element<*> {
    const { password, email } = this.state;
    const { props } = this;
    const { t, getPath } = props;

    const restoreLink = (
      <Link to={getPath(authBlockTypes.RESTORE_PASSWORD)} styleName="restore-link">
        {t('forgot?')}
      </Link>
    );

    return (
      <div>
        {this.title}
        <Form onSubmit={this.authenticate}>
          <div styleName="inputs-body">
            <FormField key="email" styleName="form-field" error={this.state.error}>
              <TextInput
                pos="top"
                placeholder={t('Email')}
                value={email}
                type="email"
                onChange={this.onChangeEmail}
                required
              />
            </FormField>
            <FormField key="passwd" styleName="form-field" error={!!this.state.error}>
              <TextInput
                type="password"
                pos="bottom"
                styleName="form-field-input"
                placeholder="Password"
                label={password ? null : restoreLink}
                value={password}
                onChange={this.onChangePassword}
                required
              />
            </FormField>
          </div>
          <Button
            type="submit"
            styleName="primary-button"
            isLoading={this.props.isLoading}
            children={t('Login')}
          />
          {this.bottomMessage}
        </Form>
      </div>
    );
  }
}

export default connect(mapState, {
  ...actions,
  fetchCart,
  saveLineItemsAndCoupons,
})(localized(Login));
