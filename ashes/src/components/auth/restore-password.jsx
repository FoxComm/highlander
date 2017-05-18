// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import styles from './css/auth.css';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

import Form from '../forms/form';
import FormField from '../forms/formfield';
import ErrorAlerts from '../alerts/error-alerts';
import { PrimaryButton } from 'components/core/button';
import PasswordInput from '../forms/password-input';
import WaitAnimation from '../common/wait-animation';

import type { SignupPayload } from 'modules/user';
import * as userActions from 'modules/user';

type State = {
  email: string,
  password1: string,
  password2: string,
}

type Props = {
  signUpState: {
    err?: any,
    inProgress?: boolean,
  },
  signUp: (payload: SignupPayload) => Promise<*>,
  isMounted: boolean,
  location: Location,
}

function mapStateToProps(state) {
  return {
    signUpState: _.get(state.asyncActions, 'signup', {})
  };
}

class RestorePassword extends Component {
  props: Props;

  state: State = {
    email: '',
  };

  @autobind
  handleSubmit() {

  }

  @autobind
  handleInputChange(event: Object) {
    const { target } = event;
    this.setState({
      [target.name]: target.value,
    });
  }

  get errorMessage(): Element<*> {
    const err = this.props.signUpState.err;
    if (!err) return null;
    return <ErrorAlerts error={err} />;
  }

  get email(): string {
    return this.state.email;
  }

  get content() {
    if (!this.props.isMounted) {
      return <WaitAnimation />;
    }

    return (
      <div styleName="main">
        <Form styleName="form" onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField styleName="signup-email" label="Email">
            <input
              name="email"
              value={this.email}
              type="email"
              className="fc-input"
            />
          </FormField>
          <PrimaryButton
            styleName="submit-button"
            type="submit"
            isLoading={this.props.signUpState.inProgress}
          >
            Restore Password
          </PrimaryButton>
        </Form>
      </div>
    );
  }

  render() {
    return (
      <div styleName="main">
        <div className="fc-auth__title">Restore Password</div>
        {this.content}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(RestorePassword);
