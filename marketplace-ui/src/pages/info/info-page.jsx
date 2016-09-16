/* @flow */

import React from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import { getInfoInProgress, getInfoFailed } from '../../core/modules';
import { submit } from '../../core/modules/merchant-info';
import { fields } from '../../forms/info/info-fields';

import type { HTMLElement } from '../../core/types';

type Props = {
  submit: Function;
  inProgress: boolean;
  failed: boolean;
}

const MerchantInfoPage = ({ submit, inProgress, failed }: Props): HTMLElement => (
  <div>
    <Header
      title="We need additional business details."
      legend="This additional information will help us ensure that we are able to add your business to our system and successfully send you payments."
    />
    <Form
      form="info"
      fields={fields}
      onSubmit={submit}
      inProgress={inProgress}
      failed={failed}
    />
  </div>
);

const mapState = state => ({
  inProgress: getInfoInProgress(state),
  failed: getInfoFailed(state),
});

export default connect(mapState, { submit })(MerchantInfoPage);
