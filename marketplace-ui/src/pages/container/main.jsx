/* @flow */

import React from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';

import { getApplicationId, getAccountId } from '../../core/modules';

import Steps from '../../components/steps/steps';

import type { HTMLElement } from '../../core/types';

import styles from './main.css';

type Props = {
  currentStep: string;
  router: Object;
  children?: HTMLElement;
}

const STEP_APPLICATION = 'application';
const STEP_ACCOUNT = 'account';
const STEP_INFO = 'info';

const steps = (currentStep, router) => [
  {
    key: STEP_APPLICATION,
    active: router.isActive(`/${STEP_APPLICATION}`),
    title: 'Apply',
    disabled: currentStep !== STEP_APPLICATION,
  },
  {
    key: STEP_ACCOUNT,
    active: router.isActive(`/${STEP_ACCOUNT}`),
    title: 'Create Account',
    disabled: currentStep !== STEP_ACCOUNT,
  },
  {
    key: STEP_INFO,
    active: router.isActive(`/${STEP_INFO}`),
    title: 'More Info',
    disabled: currentStep !== STEP_INFO,
  },
];

const Main = ({ currentStep, router, children }: Props) => (
  <main className={styles.main}>
    <Steps steps={steps(currentStep, router)} />
    {children}
  </main>
);

const getActiveStep = state => {
  const applied = !!getApplicationId(state);
  const accountCreated = !!getAccountId(state);

  if (!applied) return STEP_APPLICATION;
  if (!accountCreated) return STEP_ACCOUNT;

  return STEP_INFO;
};

const mapState = state => ({
  currentStep: getActiveStep(state),
});

export default connect(mapState)(withRouter(Main));
