import React, { PropTypes } from 'react';
import _ from 'lodash';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import { PrimaryButton, Button } from '../common/buttons';
import WaitAnimation from '../common/wait-animation.jsx'

import { transitionTo } from '../../route-helpers';
import Api from '../../lib/api';
import { autobind } from 'core-decorators';
import fetch from 'isomorphic-fetch';


export default class Login extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      email: '',
      password: '',
    }
  }

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  @autobind
  submitLogin () {
    const context = this.context;

    const headers = {'Content-Type': 'application/json;charset=UTF-8'};
    fetch(Api.apiURI('/login'), {
      method: 'POST',
      body: JSON.stringify(_.pick(this.state, 'email', 'password')),
      headers,
    }).then(response => {
      localStorage.setItem('jwt', response.headers.get('jwt'));
      localStorage.setItem('refresh-token', response.headers.get('set-refresh-token'));
      transitionTo(context.history, 'home');
    }, e => {
      console.log('login failed', e);
    });
  }

  @autobind
  onEmailChange({target}) {
    this.setState({email: target.value});
  }

  @autobind
  onPasswordChange({target}) {
    this.setState({password: target.value});
  }

  @autobind
  onForgotClick() {
    console.log('oops');
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
        <img className="fc-login__logo" src="https://s3-us-west-2.amazonaws.com/fc-ashes/images/fc-logo-nav.svg"/>
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
