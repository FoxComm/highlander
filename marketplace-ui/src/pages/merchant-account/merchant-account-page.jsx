/* @flow */

import React from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import { getApplication, getAccountInProgress, getAccountFailed } from '../../core/modules';
import { submit } from '../../core/modules/merchant-account';
import { fields } from '../../forms/merchant-account/merchant-account-fields';

import type { HTMLElement } from '../../core/types';

type Props = {
  submit: Function;
  inProgress: boolean;
  failed: boolean;
}

const MerchantAccountPage = ({ application, submit, inProgress, failed }: Props): HTMLElement => (
  <div>
    <Header
      title="Let’s get you started"
      legend="Create an Account to start earning money with BlaBla."
    />
    <Form
      form="account"
      fields={fields}
      onSubmit={submit.bind(null, 4)}
      inProgress={inProgress}
      failed={failed}
    />
  </div>
);

const mapState = state => ({
  application: getApplication(state),
  inProgress: getAccountInProgress(state),
  failed: getAccountFailed(state),
});

export default connect(mapState, { submit })(MerchantAccountPage);
