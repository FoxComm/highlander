/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import {
  getApplication,
  getApplicationFetched,
  getApplicationFetchFailed,
  getAccounts,
  getAccountSubmitInProgress,
  getAccountSubmitFailed,
} from '../../core/modules';

import { fetch as fetchApplication, clearErrors } from '../../core/modules/merchant-application';
import { fetch as fetchAccount, submit } from '../../core/modules/merchant-account';
import { fields } from '../../forms/account/account-fields';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { Accounts } from '../../core/modules/merchant-account';

type Props = {
  params: Object;
  replace: (path: string) => void;
  application: Application;
  accounts: Accounts;
  fetchAccount: (merchantId: number) => Promise<*>;
  fetchApplication: (reference: string) => Promise<*>;
  applicationFetched: boolean;
  applicationFetchFailed: boolean;
  clearErrors: () => void;
  submit: (data: Object) => Promise<*>;
  inProgress: boolean;
  submitFailed: boolean;
}

class MerchantAccountPage extends Component {
  props: Props;

  componentWillMount(): void {
    const {
      fetchApplication,
      fetchAccount,
      params: { ref },
      application,
      accounts,
      applicationFetched,
      applicationFetchFailed,
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

    if (accounts.length) {
      this.props.replace(`/application/${ref}/info`);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.accounts.length !== nextProps.accounts.length) {
      this.props.replace(`/application/${this.props.params.ref}/info`);
    }
  }

  @autobind
  submit(data) {
    const merchantId = get(this.props.application, 'merchant.id');

    if (!merchantId) {
      console.error('No merchantId');

      return;
    }

    return this.props.submit(merchantId, data);
  }

  render(): HTMLElement {
    const { inProgress, submitFailed, application } = this.props;

    if (!application.id) {
      return <span>Loading</span>;
    }

    return (
      <div>
        <Header
          title="Let’s get you started"
          legend="Create an Account to start on the GoldFish marketplace!"
        />
        <Form
          form="account"
          fields={fields}
          onSubmit={this.submit}
          inProgress={inProgress}
          failed={submitFailed}
        />
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  accounts: getAccounts(state),
  applicationFetched: getApplicationFetched(state),
  applicationFetchFailed: getApplicationFetchFailed(state),
  inProgress: getAccountSubmitInProgress(state),
  failed: getAccountSubmitFailed(state),
});

export default connect(mapState, { fetchApplication, clearErrors, fetchAccount, submit, replace })(MerchantAccountPage);
