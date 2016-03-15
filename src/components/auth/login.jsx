/** @flow */
import React, { PropTypes } from 'react';
import _ from 'lodash';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import { PrimaryButton, Button } from '../common/buttons';
import WaitAnimation from '../common/wait-animation';

import { transitionTo } from '../../route-helpers';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import * as userActions from '../../modules/user';

// types
import type {
  LoginPayload,
  TUser,
} from '../../modules/user';


type TState = {
  email: string;
  password: string;
};

type LoginProps = {
  current: TUser,
  authenticate: (payload: LoginPayload) => Promise,
}

/* ::`*/
@connect(null, userActions)
/* ::`*/
export default class Login extends React.Component {


  state: TState = {
      email: '',
      password: '',
  };

  static props: LoginProps;

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  @autobind
  submitLogin () {
    const context = this.context;
    const payload = _.pick(this.state, 'email', 'password');
    payload['kind'] = 'admin';

    this.props.authenticate(payload).then(() => {
      transitionTo(context.history, 'home');
    }).catch(err => {
      console.error(err);
    });
  }

  @autobind
  onEmailChange({target}: SyntheticInputEvent) {
    if (target instanceof HTMLInputElement) {
      this.setState({email: target.value});
    }
  }

  @autobind
  onPasswordChange({target}: SyntheticInputEvent) {
    if (target instanceof HTMLInputElement){
      this.setState({password: target.value});
    }
  }

  @autobind
  onForgotClick() {
    console.log('todo: restore password');
  }

  get passwordLabel() {
    return (
      <div className="fc-login__password-label">
        <div className="fc-login__password-label-title">Password</div>
        <a onClick={this.onForgotClick} className="fc-login__password-forgot">i forgot</a>
      </div>
    );
  }

  render() {
    return (
      <Form className="fc-grid fc-login fc-form-vertical">
        <img className="fc-login__logo" src="/images/fc-logo-v.svg"/>
        <div className="fc-login__title">Sign In</div>
        <Button className="fc-login__google-btn" icon="google">Sign In with Google</Button>
        <div className="fc-login__or">or</div>
        <div className="fc-login__or-cont"></div>
        <FormField className="fc-login__email" label="Email">
          <input onChange={this.onEmailChange} value={this.state.email} type="text" className="fc-input"/>
        </FormField>
        <FormField className="fc-login__password" label={this.passwordLabel}>
          <input onChange={this.onPasswordChange} value={this.state.password} type="password" className="fc-input"/>
        </FormField>
        <PrimaryButton className="fc-login__signin-btn" onClick={this.submitLogin}>Sign In</PrimaryButton>
        <div className="fc-login__copyright">© 2016 FoxCommerce. All rights reserved. Privacy Policy. Terms of Use.</div>
      </Form>
    );
  }
}
