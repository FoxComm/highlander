/* @flow */

import React from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import { getApplicationInProgress, getApplicationFailed } from '../../core/modules';
import { submit } from '../../core/modules/merchant-application';
import { fields } from '../../forms/merchant-application/merchant-application-fields';

import type { HTMLElement } from '../../core/types';

type Props = {
  submit: Function;
  inProgress: boolean;
  failed: boolean;
}

const MerchantApplicationPage = ({ submit, inProgress, failed }: Props): HTMLElement => (
  <div>
    <Header
      title="Apply to Sell"
      legend="Apply a merchant request to become a member of BlaBla."
    />
    <Form
      form="application"
      fields={fields}
      onSubmit={submit}
      inProgress={inProgress}
      failed={failed}
      submitText="Apply"
    />
  </div>
);

const mapState = state => ({
  inProgress: getApplicationInProgress(state),
  failed: getApplicationFailed(state),
});

export default connect(mapState, { submit })(MerchantApplicationPage);
