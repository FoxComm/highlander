/* @flow */

import _ from 'lodash';
import { get, reduce } from 'lodash';
import React, { Component } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { browserHistory, Link } from 'react-router';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import { TextInput } from 'ui/inputs';
import ShowHidePassword from 'ui/forms/show-hide-password';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'paragons/auth';

import type { HTMLElement } from 'types';
import type { SignUpPayload } from 'modules/auth';

type AuthState = {
  email: string,
  password: string,
  username: string,
  usernameError: bool|string,
  emailError: bool|string,
  generalErrors: Array<string>,
};

type Props = Localized & {
  getPath: Function,
  isLoading: boolean,
};

const mapState = state => ({
  isLoading: _.get(state.asyncActions, ['auth-signup', 'inProgress'], false),
});

class Signup extends Component {
  props: Props;

  state: AuthState = {
    email: '',
    password: '',
    username: '',
    usernameError: false,
    emailError: false,
    generalErrors: [],
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
      browserHistory.push(this.props.getPath());
    }).catch(err => {
      const errors = get(err, ['responseJson', 'errors'], [err.toString()]);
      let emailError = false;
      let usernameError = false;

      const restErrors = reduce(errors, (acc, error) => {
        if (error.indexOf('email') >= 0) {
          emailError = error;
        } else if (error.indexOf('name') >= 0) {
          usernameError = error;
        } else {
          return [...acc, error];
        }

        return acc;
      }, []);

      this.setState({
        emailError,
        usernameError,
        generalErrors: restErrors,
      });
    });
  }

  render(): HTMLElement {
    const { email, password, username, emailError, usernameError } = this.state;
    const { t, isLoading, getPath } = this.props;

    const loginLink = (
      <Link to={getPath(authBlockTypes.LOGIN)} styleName="link">
        {t('Log in')}
      </Link>
    );

    return (
      <div>
        <div styleName="title">{t('SIGN UP')}</div>
        <Form onSubmit={this.submitUser}>
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
            <ShowHidePassword
              className={styles['form-field-input']}
              placeholder={t('CREATE PASSWORD')}
              name="password"
              value={password}
              onChange={this.onChangePassword}
            />
          </FormField>
          <ErrorAlerts errors={this.state.generalErrors} />
          <Button
            styleName="primary-button"
            isLoading={isLoading}
            type="submit"
          >
            {t('SIGN UP')}
          </Button>
        </Form>
        <div styleName="switch-stage">
          {t('Already have an account?')} {loginLink}
        </div>
      </div>
    );
  }
}

export default connect(mapState, actions)(localized(Signup));
