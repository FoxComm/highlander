// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import styles from './css/auth.css';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import ErrorAlerts from 'components/alerts/error-alerts';
import { PrimaryButton } from 'components/core/button';
import PasswordInput from 'components/forms/password-input';
import WaitAnimation from 'components/common/wait-animation';
import Link from 'components/link/link';

import type { TResetPayload } from 'modules/user';
import * as userActions from 'modules/user';

type State = {
  email: string,
  dataSent: boolean,
};

type Props = {
  restoreState: {
    err?: any,
    inProgress?: boolean,
  },
  requestPasswordReset: (email: string) => Promise<*>,
  resetPassword: (payload: TResetPayload) => Promise<*>,
  isMounted: boolean,
  location: Location,
};

function mapStateToProps(state) {
  return {
    restoreState: _.get(state.asyncActions, 'requestPasswordReset', {})
  };
}

class RestorePassword extends Component {
  props: Props;

  state: State = {
    email: '',
    dataSent: false,
  };

  @autobind
  handleSubmit() {
    this.props.requestPasswordReset(this.state.email).then(() => {
      this.setState({ dataSent: true });
    });
  }

  @autobind
  handleInputChange(event: Object) {
    const { target } = event;
    this.setState({
      [target.name]: target.value,
    });
  }

  get errorMessage(): ?Element<*> {
    const err = this.props.restoreState.err;
    if (!err) return null;
    return <ErrorAlerts error={err} />;
  }

  get email(): string {
    return this.state.email;
  }

  get confirmation(): Element<*> {
    return (
      <div styleName="main">
        <div styleName="message">
          An email with reset instructions was successfully sent to&nbsp;
          <strong>{this.email}</strong>
          .
        </div>
        <div styleName="button-block">
          <Link
            to='login'
            styleName="back-button"
          >
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  get content() {
    if (!this.props.isMounted) {
      return <WaitAnimation />;
    }

    return (
      <div styleName="main">
        <div styleName="message">
          No worries! We’ll email you instructions on how to reset your password.
        </div>
        <Form styleName="form" onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField styleName="signup-email" label="Email" required>
            <input
              name="email"
              onChange={this.handleInputChange}
              value={this.email}
              type="email"
              className="fc-input"
            />
          </FormField>
          <div styleName="button-block">
            <PrimaryButton
              styleName="submit-button"
              type="submit"
              isLoading={this.props.restoreState.inProgress}
            >
              Restore Password
            </PrimaryButton>
            <Link
              to='login'
              styleName="back-button"
            >
              Back to Login
            </Link>
          </div>
        </Form>
      </div>
    );
  }

  render() {
    return (
      <div styleName="main">
        <div className="fc-auth__title">Restore Password</div>
        {!this.state.dataSent ? this.content : this.confirmation}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(RestorePassword);
