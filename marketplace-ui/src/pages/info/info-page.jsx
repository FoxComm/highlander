/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';
import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';
import Loader from '../../components/loader/loader';

import {
  getApplication,
  getApplicationFetched,
  getApplicationFetchFailed,
  getAccounts,
  getAccountsFetched,
  getInfoSubmitInProgress,
  getInfoSubmitFailed,
  getInfoSubmitSucceeded,
} from '../../core/modules';
import { fetch as fetchApplication, clearErrors } from '../../core/modules/merchant-application';
import { submit } from '../../core/modules/merchant-info';
import { fetch as fetchAccount } from '../../core/modules/merchant-account';
import { fields } from '../../forms/info/info-fields';

import styles from './info-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { Accounts } from '../../core/modules/merchant-account';

type Props = {
  params: Object;
  application: Application;
  accounts: Accounts;
  fetchApplication: (reference: string) => Promise<*>;
  fetchAccount: (merchantId: number) => Promise<*>;
  clearErrors: () => void;
  replace: (path: string) => void;
  applicationFetched: boolean;
  applicationFetchFailed: boolean;
  accountsFetched: boolean;
  submit: Function;
  inProgress: boolean;
  done: boolean;
  failed: boolean;
}

const TIMEOUT_REDIRECT = 3000;

class MerchantInfoPage extends Component {
  props: Props;

  componentWillMount(): void {
    const {
      fetchApplication,
      fetchAccount,
      params: { ref },
      application,
      applicationFetched,
      applicationFetchFailed,
      accounts,
      accountsFetched,
    } = this.props;

    if (!applicationFetched) {
      fetchApplication(ref);
    }

    if (applicationFetchFailed) {
      this.props.clearErrors();
      this.props.replace('/application');
    }

    if (applicationFetched) {
      fetchAccount(get(application, 'merchant.id'));
    }

    if (accountsFetched && !accounts.length) {
      this.props.replace(`/application/${ref}/account`);
    }
  }

  componentWillReceiveProps(nextProps: Props): void {
    if (nextProps.done) {
      setTimeout(
        () => window.location.replace(process.env.ASHES_URL),
        TIMEOUT_REDIRECT
      );
    }
  }

  @autobind
  submit(data) {
    const merchantId = get(this.props.application, 'merchant.id');

    if (!merchantId) {
      return;
    }

    return this.props.submit(merchantId, data);
  }

  get loader(): HTMLElement {
    if (!this.props.done) {
      return;
    }

    const message = <span>You're being redirected to admin page now</span>;

    return (
      <ThanksOrNot className={styles.thanksOrNot} title="You're done!" message={message}>
        <Loader />
      </ThanksOrNot>
    );
  }

  get form(): HTMLElement {
    if (this.props.done) {
      return;
    }

    const { inProgress, failed } = this.props;

    return (
      <Form
        form="info"
        fields={fields}
        onSubmit={this.submit}
        inProgress={inProgress}
        failed={failed}
      />
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.info}>
        <Header
          title="We need additional business details."
          legend={'This additional information will help us ensure that we are able to' +
                  'add your business to our system and successfully send you payments.'}
        />
        {this.loader}
        {this.form}
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  applicationFetched: getApplicationFetched(state),
  applicationFetchFailed: getApplicationFetchFailed(state),
  accounts: getAccounts(state),
  accountsFetched: getAccountsFetched(state),
  inProgress: getInfoSubmitInProgress(state),
  done: getInfoSubmitSucceeded(state),
  failed: getInfoSubmitFailed(state),
});

export default connect(mapState, { fetchApplication, fetchAccount, clearErrors, submit, replace })(MerchantInfoPage);
