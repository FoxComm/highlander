/* @flow */

import React, { Component } from 'react';

import Header from '../../components/header/header';
import ApplyForm from '../../forms/apply-form/apply-form';

import styles from './apply.css';

import type { HTMLElement } from '../../core/types';

export default (): HTMLElement => (
  <div className={styles.apply}>
    <Header
      title="Apply to Sell"
      legend="Apply a merchant request to become a member of BlaBla."
    />
    <ApplyForm onSubmit={data => console.log('submitted', data)} />
  </div>
);
