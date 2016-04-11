import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';
import { connect } from 'react-redux';

import styles from './auth.css';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Button from 'ui/buttons';
import WrapToLines from 'ui/wrap-to-lines';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'modules/auth';

import type { HTMLElement } from 'types';

import localized from 'lib/i18n';


type AuthState = {
  email: string,
  password: string,
};

const mapState = state => ({
  isLoading: state.auth.isFetching,
});

/* ::`*/
@connect(mapState, actions)
@localized
/* ::`*/
export default class Login extends Component {

  state: AuthState = {
    email: '',
    password: '',
    error: false,
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
      error: false,
    });
  }

  @autobind
  onChangePassword({target}: any) {
    this.setState({
      password: target.value,
      error: false,
    });
  }

  @autobind
  authenticate(e: any) {
    e.preventDefault();
    e.stopPropagation();
    const { email, password } = this.state;
    const kind = 'customer';
    this.props.authenticate({email, password, kind}).then(() => {
      browserHistory.push('/');
    }).catch(() => {
      this.setState({error: 'Email or password is invalid'});
    });
  }

  render(): HTMLElement {
    const { password, email } = this.state;
    const props = this.props;
    const { t } = props;

    const restoreLink = (
      <a styleName="restore-link" onClick={() => props.changeAuthBlockType(authBlockTypes.RESTORE_PASSWORD)}>
        {t('forgot?')}
      </a>
    );

    const signupLink = (
      <a styleName="signup-link" onClick={() => props.changeAuthBlockType(authBlockTypes.SIGNUP)}>
        {t('Sign Up')}
      </a>
    );

    return (
      <div>
        <div styleName="title">{t('LOG IN')}</div>
        <form>
          <Button icon="fc-google" onClick={props.googleSignin} type="button" styleName="google-login">
            {t('LOG IN WITH GOOGLE')}
          </Button>
        </form>
        <WrapToLines styleName="divider">{t('or')}</WrapToLines>
        <form>
          <FormField key="email" styleName="form-field" error={this.state.error}>
            <TextInput placeholder={t('EMAIL')} value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field" error={!!this.state.error}>
            <TextInputWithLabel
              placeholder="PASSWORD"
              label={!password && restoreLink}
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button
            styleName="primary-button"
            isLoading={props.isLoading}
            onClick={this.authenticate}
          >
            {t('LOG IN')}
          </Button>
        </form>
        <div styleName="switch-stage">
          {t('Don’t have an account?')} {signupLink}
        </div>
      </div>
    );
  }
}
