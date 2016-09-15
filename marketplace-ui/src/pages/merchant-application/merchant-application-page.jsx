/* @flow */

import React from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import MerchantApplicationForm from '../../forms/merchant-application/merchant-application-form';

import { getApplyFormActionInProgress, getApplyFormActionFailed } from '../../core/modules';
import { submit } from '../../core/modules/merchant-application';

import styles from './merchant-application-page.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  submit: Function;
  inProgress: boolean;
  failed: boolean;
}

const MerchantApplicationPage = ({ submit, inProgress, failed }: Props): HTMLElement => (
  <div className={styles.apply}>
    <Header
      title="Apply to Sell"
      legend="Apply a merchant request to become a member of BlaBla."
    />
    <MerchantApplicationForm onSubmit={submit} inProgress={inProgress} failed={failed} />
  </div>
);

const mapState = state => ({
  inProgress: getApplyFormActionInProgress(state),
  failed: getApplyFormActionFailed(state),
});

export default connect(mapState, { submit })(MerchantApplicationPage);
