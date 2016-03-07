/* @flow */

import React, { Component, PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import {reduxForm} from 'redux-form';
import { connect } from 'react-redux';
import { routeActions } from 'react-router-redux';

import { TextInput } from '../../common/inputs';
import { FormField } from '../../forms';
import Button from '../../common/buttons';

import type { HTMLElement } from '../../../types';

type ResetState = {
  isReseted: boolean;
};

type FormData = {
  passwd1: string;
  passwd2: string;
}

const validate = values => {
  const errors = {};
  if (!values.passwd1) {
    errors.passwd1 = 'Required';
  } else if (values.passwd1.length < 8) {
    errors.passwd1 = 'Must be 8 characters or more';
  }
  if (!values.passwd2) {
    errors.passwd2 = 'Required';
  } else if (values.passwd2.length < 8) {
    errors.passwd2 = 'Must be 8 characters or more';
  }
  return errors;
};

/* ::`*/
@connect()
@reduxForm({
  form: 'reset-password',
  validate,
  fields: ['passwd1', 'passwd2'],
})
@cssModules(styles)
/* ::`*/
export default class ResetPassword extends Component {

  static propTypes = {
    fields: PropTypes.object.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    submitting: PropTypes.bool.isRequired,
    error: PropTypes.string,
    dispatch: PropTypes.func.isRequired,
  };

  state: ResetState = {
    isReseted: false,
  };

  @autobind
  handleSubmit(data: FormData): Promise {
    if (data.passwd1 != data.passwd2) {
      return Promise.reject({
        _error: 'Passwords must match',
      });
    }
    this.setState({
      isReseted: true,
    });

    return Promise.resolve({_error: null});
  }

  get topMessage(): HTMLElement {
    const { isReseted } = this.state;
    const { error } = this.props;

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

  get passwordFields(): ?HTMLElement[] {
    const { isReseted } = this.state;
    const { fields: {passwd1, passwd2}} = this.props;

    if (isReseted) return null;

    return [
      <FormField key="passwd1" styleName="form-field" {...passwd1}>
        <TextInput placeholder="NEW PASSWORD" required type="password" minLength="8" {...passwd1} />
      </FormField>,
      <FormField key="passwd2" styleName="form-field" {...passwd2}>
        <TextInput placeholder="CONFIRM PASSWORD" required type="password" minLength="8" {...passwd2} />
      </FormField>,
    ];
  }

  @autobind
  gotoLogin() {
    this.props.dispatch(routeActions.push('/login'));
  }

  get primaryButton(): HTMLElement {
    const { isReseted } = this.state;

    if (isReseted) {
      return (
        <Button styleName="primary-button" onClick={this.gotoLogin}>BACK TO LOG IN</Button>
      );
    }

    return <Button styleName="primary-button" type="submit">RESET PASSWORD</Button>;
  }

  render(): HTMLElement {
    const { handleSubmit } = this.props;

    return (
      <div>
        <div styleName="title">RESET PASSWORD</div>
        {this.topMessage}
        <form onSubmit={handleSubmit(this.handleSubmit)}>
          {this.passwordFields}
          {this.primaryButton}
        </form>
      </div>
    );
  }
}
