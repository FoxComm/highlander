/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';
import { authBlockTypes } from 'paragons/auth';

import localized from 'lib/i18n';

import ShowHidePassword from 'ui/forms/show-hide-password';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';

import { resetPassword } from 'modules/auth';

type ResetState = {
  isReseted: boolean;
  passwd1: string;
  passwd2: string;
  error: ?string;
};

/* ::`*/
@connect(null, { resetPassword })
@localized
/* ::`*/
export default class ResetPassword extends Component {
  state: ResetState = {
    isReseted: false,
    passwd1: '',
    passwd2: '',
    error: null,
  };

  @autobind
  handleSubmit(): ?Promise<*> {
    const { passwd1, passwd2 } = this.state;
    const code = _.get(this.props, 'path.query.code');

    if (passwd1 != passwd2) {
      this.setState({
        error: this.props.t('Passwords must match'),
      });

      return Promise.reject({
        password: 'Passwords must match',
      });
    }

    if (code == null || _.isEmpty(code)) {
      this.setState({
        error: this.props.t('Code cannot be empty'),
      });

      return Promise.reject({
        code: 'Code cannot be empty',
      });
    }

    return this.props.resetPassword(code, passwd1).then(() => {
      this.setState({
        isReseted: true,
        error: null,
      });
    }).catch(() => {
      return this.setState({
        error: this.props.t('Passwords do not match or security code is invalid.'),
      });
    });
  }

  get topMessage(): Element<*> {
    const { isReseted, error } = this.state;
    const { t } = this.props;

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
          {t('Your password was successfully reset.')}
        </div>
      );
    }

    return (
      <div styleName="top-message">
        {t('Your new password must be at least 8 characters long.')}
      </div>
    );
  }

  @autobind
  updateForm({target}: any) {
    this.setState({
      [target.name]: target.value,
    });
  }

  get passwordFields(): ?Element<*>[] {
    const { isReseted, passwd1, passwd2, error } = this.state;
    const { t } = this.props;

    if (isReseted) return null;

    return [
      <FormField key="passwd1" styleName="form-field" error={!!error}>
        <ShowHidePassword
          pos="top"
          className={styles['form-field-input']}
          placeholder={t('New password')}
          type="password"
          minLength={8}
          value={passwd1}
          name="passwd1"
          onChange={this.updateForm}
          required
        />
      </FormField>,
      <FormField key="passwd2" styleName="form-field" error={!!error}>
        <ShowHidePassword
          pos="bottom"
          className={styles['form-field-input']}
          placeholder={t('Confirm password')}
          type="password"
          minLength={8}
          value={passwd2}
          name="passwd2"
          onChange={this.updateForm}
          required
        />
      </FormField>,
    ];
  }

  goToLogin() {
    browserHistory.push(this.props.getPath(authBlockTypes.LOGIN));
  }

  get primaryButton(): Element<*> {
    const { isReseted } = this.state;
    const { t } = this.props;

    if (isReseted) {
      return (
        <Button styleName="primary-button" type="button" onClick={() => this.goToLogin()}>{t('Back to log in')}</Button>
      );
    }

    return <Button styleName="primary-button" type="submit">{t('Reset password')}</Button>;
  }

  render(): Element<*> {
    const { t } = this.props;

    return (
      <div>
        <div styleName="title">{t('Reset password')}</div>
        {this.topMessage}
        <Form onSubmit={this.handleSubmit}>
          <div styleName="inputs-body">
            {this.passwordFields}
          </div>
          {this.primaryButton}
        </Form>
      </div>
    );
  }
}
