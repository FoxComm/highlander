/* @flow */

import React, { Component } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Button from 'ui/buttons';
import WrapToLines from 'ui/wrap-to-lines';
import { Link } from 'react-router';

import type { HTMLElement } from 'types';


type AuthState = {
  email: string,
  password: string,
};

export default class Auth extends Component {

  state: AuthState = {
    email: '',
    password: '',
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  onChangePassword({target}: any) {
    this.setState({
      password: target.value,
    });
  }

  render(): HTMLElement {
    const { password, email } = this.state;

    return (
      <div>
        <div styleName="title">LOG IN</div>
        <Button icon="fc-google" styleName="google-login">LOG IN WITH GOOGLE</Button>
        <WrapToLines styleName="divider">or</WrapToLines>
        <form>
          <FormField key="email" styleName="form-field">
            <TextInput placeholder="EMAIL" value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field">
            <TextInputWithLabel placeholder="PASSWORD"
              label={!password && <Link styleName="restore-link" to="/password/restore">forgot ?</Link>}
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button styleName="primary-button">LOG IN</Button>
        </form>
        <div styleName="switch-stage">
          Don’t have an account? <Link styleName="signup-link" to="/signup">Sign Up</Link>
        </div>
      </div>
    );
  }
}
