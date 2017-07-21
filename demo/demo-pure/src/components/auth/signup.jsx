/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import { browserHistory } from 'lib/history';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import TextInput from 'ui/text-input/text-input';
import ShowHidePassword from 'ui/forms/show-hide-password';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';
import ErrorAlerts from 'ui/alerts/error-alerts';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'paragons/auth';
import { fetch as fetchCart, saveLineItemsAndCoupons } from 'modules/cart';

import type { SignUpPayload } from 'modules/auth';

type AuthState = {
  email: string,
  password: string,
  username: string,
  usernameError: bool|string,
  emailError: bool|string,
  generalErrors: Array<string>,
};

type Props = Localized & {
  getPath: Function,
  isLoading: boolean,
  fetchCart: Function,
  saveLineItemsAndCoupons: Function,
  onLoginClick: Function,
  title?: string|Element<*>|null,
  mergeGuestCart: boolean,
  onAuthenticated?: Function,
};

const mapState = state => ({
  cart: state.cart,
  isLoading: _.get(state.asyncActions, ['auth-signup', 'inProgress'], false),
});

class Signup extends Component {
  props: Props;

  state: AuthState = {
    email: '',
    password: '',
    username: '',
    usernameError: false,
    emailError: false,
    generalErrors: [],
  };

  static defaultProps = {
    mergeGuestCart: false,
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
      emailError: false,
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
      usernameError: false,
    });
  }

  @autobind
  submitUser() {
    const {email, password, username: name} = this.state;
    const payload: SignUpPayload = {email, password, name};
    const signUp = this.props.signUp(payload).then(() => {
      const lineItems = _.get(this.props, 'cart.lineItems', []);
      const couponCode = _.get(this.props, 'cart.coupon.code', null);
      if (_.isEmpty(lineItems) && _.isNil(couponCode)) {
        this.props.fetchCart();
      } else {
        this.props.saveLineItemsAndCoupons(this.props.mergeGuestCart);
      }
      browserHistory.push(this.props.getPath());
    }).catch((err) => {
      const errors = _.get(err, ['responseJson', 'errors'], [err.toString()]);
      let emailError = false;
      let usernameError = false;

      const restErrors = _.reduce(errors, (acc, error) => {
        if (error.indexOf('email') >= 0) {
          emailError = error;
        } else if (error.indexOf('name') >= 0) {
          usernameError = error;
        } else {
          return [...acc, error];
        }

        return acc;
      }, []);

      this.setState({
        emailError,
        usernameError,
        generalErrors: restErrors,
      });
    });

    if (this.props.onAuthenticated) {
      signUp.then(this.props.onAuthenticated);
    }
  }

  get title() {
    const { t, title } = this.props;
    return title !== null
      ? <div styleName="title">{title || t('Sign up')}</div>
      : null;
  }

  get bottomMessage() {
    const { props } = this;
    const { t } = props;

    return (
      <div styleName="bottom-message">
        <Link to={props.getPath(authBlockTypes.LOGIN)} onClick={props.onLoginClick} styleName="link">
          {t('Already have an account?')}
        </Link>
      </div>
    );
  }

  render(): Element<*> {
    const { email, password, username, emailError, usernameError } = this.state;
    const { t, isLoading } = this.props;

    return (
      <div>
        {this.title}
        <Form onSubmit={this.submitUser}>
          <div styleName="inputs-body">
            <ErrorAlerts errors={this.state.generalErrors} />
            <FormField key="username" styleName="form-field" error={usernameError}>
              <TextInput
                pos="top"
                required
                placeholder={t('First & last name')}
                name="username"
                value={username}
                onChange={this.onChangeUsername}
              />
            </FormField>
            <FormField key="email" styleName="form-field" error={emailError}>
              <TextInput
                pos="middle-v"
                required
                placeholder={t('Email')}
                name="email"
                value={email}
                type="email"
                onChange={this.onChangeEmail}
              />
            </FormField>
            <FormField key="passwd" styleName="form-field" required>
              <ShowHidePassword
                pos="bottom"
                className={styles['form-field-input']}
                placeholder={t('Create password')}
                name="password"
                value={password}
                onChange={this.onChangePassword}
              />
            </FormField>
          </div>
          <Button
            styleName="primary-button"
            isLoading={isLoading}
            type="submit"
            children={t('Sign up')}
          />
          {this.bottomMessage}
        </Form>
      </div>
    );
  }
}

export default connect(mapState, {
  ...actions,
  fetchCart,
  saveLineItemsAndCoupons,
})(localized(Signup));
