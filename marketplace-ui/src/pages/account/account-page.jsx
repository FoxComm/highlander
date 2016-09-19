/* @flow */

import get from 'lodash/get';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import { getApplication, getAccounts, getAccountInProgress, getAccountFailed } from '../../core/modules';
import { fetch as fetchApplication } from '../../core/modules/merchant-application';
import { fetch, submit } from '../../core/modules/merchant-account';
import { fields } from '../../forms/account/account-fields';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { Accounts } from '../../core/modules/merchant-account';

type Props = {
  params: Object;
  replace: (path: string) => void;
  application: Application;
  accounts: Accounts;
  fetch: (merchantId: number) => Promise<*>;
  fetchApplication: (reference: string) => Promise<*>;
  submit: (data: Object) => Promise<*>;
  inProgress: boolean;
  submitFailed: boolean;
}

class MerchantAccountPage extends Component {
  props: Props;

  componentWillMount(): void {
    const { fetchApplication, fetch, params: { ref: refParam }, application, accounts } = this.props;

    if (refParam && !application.reference_number) {
      fetchApplication(refParam)
        .then((application: Application) =>
          fetch(get(application, 'merchant.id'))
        );
    }

    if (application.reference_number) {
      fetch(get(application, 'merchant.id'));
    }

    if (accounts.length) {
      this.props.replace(`/application/${refParam}/info`);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.accounts.length !== nextProps.accounts.length) {
      this.props.replace(`/application/${this.props.params.ref}/info`);
    }
  }

  render(): HTMLElement {
    const { submit, inProgress, submitFailed, application } = this.props;

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
          onSubmit={submit.bind(null, get(application, 'merchant.id'))}
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
  inProgress: getAccountInProgress(state),
  failed: getAccountFailed(state),
});

export default connect(mapState, { fetchApplication, fetch, submit, replace })(MerchantAccountPage);
