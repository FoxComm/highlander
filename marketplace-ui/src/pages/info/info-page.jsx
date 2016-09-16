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
      title="Help us send you money"
      legend="Provide more info about you business to earn more money with BlaBla."
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
