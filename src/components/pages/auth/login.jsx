/* @flow */

import React, { Component, Element } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';

import { TextInput, TextInputWithLabel } from '../../common/inputs';
import { FormField } from '../../forms';
import Button from '../../common/buttons';
import WrapToLines from '../../common/wrap-to-lines';
import { Link } from 'react-router';


type AuthState = {
  email: string,
  password: string,
};

/* ::`*/
@cssModules(styles)
/* ::`*/
export default class Auth extends Component {

  state: AuthState = {
    email: '',
    password: '',
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

  render(): Element {
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
              label={!password && <Link to="/login/restore">restore ?</Link>}
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button styleName="primary-button">LOG IN</Button>
        </form>
        <div styleName="switch-stage">
          Don’t have an account? <Link to="/signup">Sign Up</Link>
        </div>
      </div>
    );
  }
}
