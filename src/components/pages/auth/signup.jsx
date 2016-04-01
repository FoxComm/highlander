/* @flow weak */

// import _ from 'lodash';
import React, { Component } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { browserHistory } from 'react-router';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';
import WrapToLines from 'ui/wrap-to-lines';
import { Link } from 'react-router';

import * as actions from 'modules/auth';

import type { HTMLElement } from 'types';
import type { SignUpPayload } from 'modules/auth';

type AuthState = {
  email: string,
  password: string,
  username: string
};

/* ::`*/
@connect(null, actions)
/* ::`*/
export default class Auth extends Component {

  state: AuthState = {
    email: '',
    password: '',
    username: '',
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

  @autobind
  onChangeUsername({target}: any) {
    this.setState({
      username: target.value,
    });
  }

  @autobind
  submitUser() {
    const {email, password, username: name} = this.state;
    const paylaod: SignUpPayload = {email, password, name};
    this.props.signUp(paylaod).then(() => {
      browserHistory.push('/login');
    }).catch(err => {
      console.error(err);
    });
  }

  render(): HTMLElement {
    const { email, password, username } = this.state;

    return (
      <div>
        <div styleName="title">SIGN UP</div>
        <Button icon="fc-google" type="button" styleName="google-login">SIGN UP WITH GOOGLE</Button>
        <WrapToLines styleName="divider">or</WrapToLines>
        <Form onSubmit={this.submitUser} >
          <FormField key="username" styleName="form-field">
            <TextInput required placeholder="FIRST & LAST NAME" name="username" value={username} onChange={this.onChangeUsername} />
          </FormField>
          <FormField key="email" styleName="form-field">
            <TextInput required placeholder="EMAIL" name="email" value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field">
            <TextInputWithLabel required placeholder="CREATE PASSWORD" name="password"
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button styleName="primary-button" type="submit">SIGN UP</Button>
        </Form>
        <div styleName="switch-stage">
          Already have an account? <Link to="/login">Log In</Link>
        </div>
      </div>
    );
  }
}
