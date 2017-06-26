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
  getAccounts,
  getAccountSubmitInProgress,
  getAccountSubmitFailed
} from '../../core/modules';

import { submitAccount as submit } from '../../core/modules/merchant-account';
import { fields as accountFields } from '../../forms/account/account-fields';

import styles from './account-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { Accounts } from '../../core/modules/merchant-account';

type Props = {
  application: Application;
  accounts: Accounts;
  submit: (data: Object) => Promise<*>;
  inProgress: boolean;
  submitFailed: boolean;
  params: { ref: string };
  replace: (path: string) => void;
}

const fields = [...accountFields];

class MerchantAccountPage extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.accounts.length !== nextProps.accounts.length) {
      this.props.replace(`/application/${this.props.params.ref}/actions`);
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
    const { inProgress, submitFailed } = this.props;
    const title = `Welcome ${this.props.application.business_name}!`;

    return (
      <div className={styles.account}>
        <Header
          title={title}
          legend="Create an Admin Account"
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
  inProgress: getAccountSubmitInProgress(state),
  failed: getAccountSubmitFailed(state),
});

export default connect(mapState, { submit, replace })(MerchantAccountPage);
