/* @flow */

import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';

import { TextInput, TextInputWithLabel } from '../../common/inputs';
import { FormField } from '../../forms';
import Button from '../../common/buttons';
import WrapToLines from '../../common/wrap-to-lines';
import { Link } from 'react-router';

import type { HTMLElement } from '../../../types';

type AuthState = {
  email: string,
  password: string,
  username: string
};

/* ::`*/
@cssModules(styles)
/* ::`*/
export default class Auth extends Component {

  state: AuthState = {
    email: '',
    password: '',
    username: '',
  };

  @autobind
  onChangeEmail({target}: SEvent<HTMLInputElement>) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  onChangePassword({target}: SEvent<HTMLInputElement>) {
    this.setState({
      password: target.value,
    });
  }

  @autobind
  onChangeUsername({target}: SEvent<HTMLInputElement>) {
    this.setState({
      username: target.value,
    });
  }

  render(): HTMLElement {
    const { email, password, username } = this.state;

    return (
      <div>
        <div styleName="title">SIGN UP</div>
        <Button icon="fc-google" styleName="google-login">SIGN UP WITH GOOGLE</Button>
        <WrapToLines styleName="divider">or</WrapToLines>
        <form>
          <FormField key="email" styleName="form-field">
            <TextInput placeholder="EMAIL" value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field">
            <TextInputWithLabel placeholder="CREATE PASSWORD"
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <FormField key="username" styleName="form-field">
            <TextInput placeholder="FIRST & LAST NAME" value={username} onChange={this.onChangeUsername} />
          </FormField>
          <Button styleName="primary-button">SIGN UP</Button>
        </form>
        <div styleName="switch-stage">
          Already have an account? <Link to="/login">Log In</Link>
        </div>
      </div>
    );
  }
}
