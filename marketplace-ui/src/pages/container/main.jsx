/* @flow */

import React from 'react';
import { Link, withRouter } from 'react-router';

import Tabs from '../../components/tabs/tabs';

import type { HTMLElement } from '../../core/types';

import styles from './main.css';

type Props = {
  router: Object;
  children?: HTMLElement;
}

const tabs = router => [
  {
    key: 'application',
    active: router.isActive('/application'),
    title: 'Apply',
  },
  {
    key: 'account',
    active: router.isActive('/account'),
    title: 'Create Account',
  },
  {
    key: 'info',
    active: router.isActive('/info'),
    title: 'More Info',
  },
];

export default withRouter(({ router, children }: Props) => (
  <main className={styles.main}>
    <Tabs
      tabs={tabs(router)}
      onChange={tab => router.push(tab)}
    />
    {children}
  </main>
));
