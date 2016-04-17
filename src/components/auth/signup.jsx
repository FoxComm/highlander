/* @flow weak */

import { each, get } from 'lodash';
import React, { Component, PropTypes } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { browserHistory, Link } from 'react-router';

import localized from 'lib/i18n';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';
import WrapToLines from 'ui/wrap-to-lines';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'modules/auth';

import type { HTMLElement } from 'types';
import type { SignUpPayload } from 'modules/auth';

type AuthState = {
  email: string,
  password: string,
  username: string,
  usernameError: bool|string,
  emailError: bool|string,
};

/* ::`*/
@connect(null, actions)
@localized
/* ::`*/
export default class Auth extends Component {
  static propTypes = {
    path: PropTypes.string,
  };

  state: AuthState = {
    email: '',
    password: '',
    username: '',
    usernameError: false,
    emailError: false,
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
      emailError: false,
    });
  }

  @autobind
  onChangePassword({target}: any) {
    this.setState({
      password: target.value,
    });
  }

  @autobind
  onChangeUsername({target}: any) {
    this.setState({
      username: target.value,
      usernameError: false,
    });
  }

  @autobind
  submitUser() {
    const {email, password, username: name} = this.state;
    const paylaod: SignUpPayload = {email, password, name};
    this.props.signUp(paylaod).then(() => {
      browserHistory.push({
        pathname: this.props.path,
        query: {auth: authBlockTypes.LOGIN},
      });
    }).catch(err => {
      const errors = get(err, ['responseJson', 'errors'], []);
      let emailError = false;
      let usernameError = false;
      each(errors, error => {
        if (error.indexOf('email') >= 0) {
          emailError = error;
        }
        if (error.indexOf('name') >= 0) {
          usernameError = error;
        }
      });
      this.setState({emailError, usernameError});
    });
  }

  render(): HTMLElement {
    const { email, password, username, emailError, usernameError } = this.state;
    const { t } = this.props;

    const loginLink = (
      <Link to={{pathname: this.props.path, query: {auth: authBlockTypes.LOGIN}}} styleName="link">
        {t('Log in')}
      </Link>
    );

    return (
      <div>
        <div styleName="title">{t('SIGN UP')}</div>
        <Button icon="fc-google" type="button" styleName="google-login">{t('SIGN UP WITH GOOGLE')}</Button>
        <WrapToLines styleName="divider">{t('or')}</WrapToLines>
        <Form onSubmit={this.submitUser} >
          <FormField key="username" styleName="form-field" error={usernameError}>
            <TextInput
              required
              placeholder={t('FIRST & LAST NAME')}
              name="username"
              value={username}
              onChange={this.onChangeUsername}
            />
          </FormField>
          <FormField key="email" styleName="form-field" error={emailError}>
            <TextInput
              required
              placeholder={t('EMAIL')}
              name="email"
              value={email}
              type="email"
              onChange={this.onChangeEmail}
            />
          </FormField>
          <FormField key="passwd" styleName="form-field">
            <TextInputWithLabel
              required
              placeholder={t('CREATE PASSWORD')}
              name="password"
              value={password}
              onChange={this.onChangePassword}
              type="password"
            />
          </FormField>
          <Button styleName="primary-button" type="submit">{t('SIGN UP')}</Button>
        </Form>
        <div styleName="switch-stage">
          {t('Already have an account?')} {loginLink}
        </div>
      </div>
    );
  }
}
