// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import { ApiErrors } from 'components/utils/errors';
import { PrimaryButton } from 'components/core/button';
import { Link } from 'components/link';
import TextInput from 'components/core/text-input';

import * as userActions from 'modules/user';

import s from './css/auth.css';

import type { TResetPayload } from 'modules/user';

type State = {
  email: string,
  dataSent: boolean,
};

type Props = {
  restoreState: AsyncState,
  requestPasswordReset: (email: string) => Promise<*>,
  resetPassword: (payload: TResetPayload) => Promise<*>,
  isMounted: boolean,
  location: Location,
};

function mapStateToProps(state) {
  return {
    restoreState: _.get(state.asyncActions, 'requestPasswordReset', {}),
  };
}

function sanitize(err: string): string {
  if (err.startsWith('user with key=') && err.endsWith('not found')) {
    return 'We don’t have a user with that email. Please check your entry and try again.';
  }
  return err;
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
  handleInputChange(value: string) {
    this.setState({
      email: value,
    });
  }

  get errorMessage(): ?Element<*> {
    const err = this.props.restoreState.err;

    if (!err) return null;

    return <ApiErrors error={err} sanitizeError={sanitize} />;
  }

  get email(): string {
    return this.state.email;
  }

  get confirmation(): Element<*> {
    return (
      <div className={s.main}>
        <div className={s.message}>
          An email with reset instructions was successfully sent to&nbsp;
          <strong>{this.email}</strong>
          .
        </div>
        <div className={s.buttonBlock}>
          <Link to="login" className={s.backButton}>
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  get content(): Element<*> {
    return (
      <div className={s.main}>
        <div className={s.message}>
          No worries! We’ll email you instructions on how to reset your password.
        </div>
        <Form className={s.form} onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField className={s.signupEmail} label="Email" required>
            <TextInput
              name="email"
              onChange={this.handleInputChange}
              value={this.email}
              type="email"
              className="fc-input"
            />
          </FormField>
          <div className={s.buttonBlock}>
            <PrimaryButton className={s.submitButton} type="submit" isLoading={this.props.restoreState.inProgress}>
              Restore Password
            </PrimaryButton>
            <Link to="login" className={s.backButton}>
              Back to Login
            </Link>
          </div>
        </Form>
      </div>
    );
  }

  render() {
    return (
      <div className={s.main}>
        <div className="fc-auth__title">Restore Password</div>
        {!this.state.dataSent ? this.content : this.confirmation}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(RestorePassword);
