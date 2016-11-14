/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { browserHistory, Link } from 'react-router';

import { authBlockTypes } from 'paragons/auth';

import localized from 'lib/i18n';

import { TextInput } from 'ui/inputs';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';

import { restorePassword } from 'modules/auth';

import type { HTMLElement } from 'types';

type RestoreState = {
  emailSent: boolean;
  error: ?string;
  email: string;
};

export type RestorePasswordFormProps = {
  fields: Object,
  handleSubmit: Function,
  resetForm: Function,
  submitting: boolean,
  error: string,
  dispatch: ?Function,
  changeAuthBlockType: ?Function,
  getPath: Function,
  topMessage: string,
  title: string,
  t: Function,
  restorePassword: Function,
};

/* ::`*/
@connect(null, { restorePassword })
@localized
/* ::`*/
export default class RestorePasswordForm extends Component {
  props: RestorePasswordFormProps;

  state: RestoreState = {
    emailSent: false,
    error: null,
    email: '',
  };

  @autobind
  handleSubmit(): ?Promise {
    const { email } = this.state;
    const { t } = this.props;

    if (_.isEmpty(email)) {
      return Promise.reject({
        email: t('Oops! We don’t have a user with that email. Please check your entry and try again.'),
      });
    }

    return this.props.restorePassword(email)
      .then(() => {
        this.setState({
          emailSent: true,
          error: null,
        });
      }).catch(() => {
        this.setState({
          error: t(`Oops! We don’t have a user with that email. Please check your entry and try again.`),
        });
      }
    );
  }

  get topMessage(): HTMLElement {
    const { emailSent, error, email } = this.state;
    const { t } = this.props;

    if (error) {
      return (
        <div styleName="top-message">
          {error}
        </div>
      );
    }

    if (emailSent) {
      return (
        <div styleName="top-message">
          {t('An email was successfully sent to')} <strong>{email}</strong> {t('with reset instructions')}!
        </div>
      );
    }

    return (
      <div styleName="top-message">
        {this.props.topMessage}
      </div>
    );
  }

  @autobind
  changeEmail({target}: any) {
    this.setState({
      email: target.value,
    });
  }

  get emailField(): ?HTMLElement {
    const { emailSent, email } = this.state;
    const { t } = this.props;

    if (emailSent) return null;

    return (
      <FormField name="email" key="email" styleName="form-field">
        <TextInput placeholder={t('EMAIL')} required type="email" value={email} onChange={this.changeEmail} />
      </FormField>
    );
  }

  goToLogin: Object = () => {
    browserHistory.push(this.props.getPath(authBlockTypes.LOGIN));
  };

  get primaryButton(): HTMLElement {
    const { emailSent } = this.state;
    const { t } = this.props;

    if (emailSent) {
      return (
        <Button styleName="primary-button" onClick={this.goToLogin} type="button">
          {t('BACK TO SIGN IN')}
        </Button>
      );
    }

    return <Button styleName="primary-button" type="submit">{t('SUBMIT')}</Button>;
  }

  get switchStage(): ?HTMLElement {
    const { emailSent } = this.state;
    const { t, getPath } = this.props;

    if (!emailSent) {
      return (
        <div styleName="switch-stage">
          <Link to={getPath(authBlockTypes.LOGIN)} styleName="link">
            {t('BACK TO SIGN IN')}
          </Link>
        </div>
      );
    }
  }

  render(): HTMLElement {
    return (
      <div>
        <div styleName="title">{this.props.title}</div>
        {this.topMessage}
        <Form onSubmit={this.handleSubmit}>
          {this.emailField}
          {this.primaryButton}
        </Form>
        {this.switchStage}
      </div>
    );
  }
}
