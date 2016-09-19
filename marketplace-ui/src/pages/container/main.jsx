/* @flow */

import React from 'react';
import { withRouter } from 'react-router';

import Steps from '../../components/steps/steps';

import type { HTMLElement } from '../../core/types';

import styles from './main.css';

type Props = {
  location: Object;
  children?: HTMLElement;
}

const STEP_APPLICATION = 'application';
const STEP_ACCOUNT = 'account';
const STEP_INFO = 'info';

const steps = (pathname) => [
  {
    key: STEP_APPLICATION,
    active: /^\/application(\/(\w+\-?)*\/?)?$/.test(pathname),
    title: 'Apply',
  },
  {
    key: STEP_ACCOUNT,
    active: /^\/application\/(\w+\-?)+\/account\/?$/.test(pathname),
    title: 'Create Account',
  },
  {
    key: STEP_INFO,
    active: /^\/application\/(\w+\-?)+\/info\/?$/.test(pathname),
    title: 'More Info',
  },
];

const Main = ({ location: { pathname }, children }: Props) => (
  <main className={styles.main}>
    <Steps steps={steps(pathname)} />
    {children}
  </main>
);

export default withRouter(Main);
