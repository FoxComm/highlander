/* @flow */

import React, { Component, PropTypes } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { routeActions } from 'react-router-redux';
import { authBlockTypes } from 'modules/auth';

import { TextInput } from 'ui/inputs';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';

import type { HTMLElement } from 'types';

type ResetState = {
  isReseted: boolean;
  passwd1: string;
  passwd2: string;
  error: ?string;
};

/* ::`*/
@connect()
/* ::`*/
export default class ResetPassword extends Component {

  static propTypes = {
    fields: PropTypes.object.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    submitting: PropTypes.bool.isRequired,
    dispatch: PropTypes.func.isRequired,
  };

  state: ResetState = {
    isReseted: false,
    passwd1: '',
    passwd2: '',
    error: null,
  };

  @autobind
  handleSubmit(): ?Promise {
    const { passwd1, passwd2 } = this.state;

    if (passwd1 != passwd2) {
      this.setState({
        error: 'Passwords must match',
      });
    } else {
      this.setState({
        isReseted: true,
        error: null,
      });
    }
  }

  get topMessage(): HTMLElement {
    const { isReseted, error } = this.state;

    if (error) {
      return (
        <div styleName="top-message-error">
          {error}
        </div>
      );
    }

    if (isReseted) {
      return (
        <div styleName="top-message-success">
          Woohoo! Your password was successfully reset.
        </div>
      );
    }

    return (
      <div styleName="top-message">
        Your new password must be at least 8 characters long.
      </div>
    );
  }

  @autobind
  updateForm({target}: any) {
    this.setState({
      [target.name]: target.value,
    });
  }

  get passwordFields(): ?HTMLElement[] {
    const { isReseted, passwd1, passwd2, error } = this.state;

    if (isReseted) return null;

    return [
      <FormField key="passwd1" styleName="form-field" error={!!error}>
        <TextInput
          placeholder="NEW PASSWORD"
          required
          type="password"
          minLength="8"
          value={passwd1}
          name="passwd1"
          onChange={this.updateForm}
        />
      </FormField>,
      <FormField key="passwd2" styleName="form-field" error={!!error}>
        <TextInput placeholder="CONFIRM PASSWORD" required type="password" minLength="8"
          value={passwd2}
          name="passwd2"
          onChange={this.updateForm}
        />
      </FormField>,
    ];
  }

  @autobind
  gotoLogin() {
    this.props.dispatch(routeActions.push({
      pathname: this.props.path,
      query: {auth: authBlockTypes.LOGIN},
    }));
  }

  get primaryButton(): HTMLElement {
    const { isReseted } = this.state;

    if (isReseted) {
      return (
        <Button styleName="primary-button" type="button" onClick={this.gotoLogin}>BACK TO LOG IN</Button>
      );
    }

    return <Button styleName="primary-button" type="submit">RESET PASSWORD</Button>;
  }

  render(): HTMLElement {
    return (
      <div>
        <div styleName="title">RESET PASSWORD</div>
        {this.topMessage}
        <Form onSubmit={this.handleSubmit}>
          {this.passwordFields}
          {this.primaryButton}
        </Form>
      </div>
    );
  }
}
