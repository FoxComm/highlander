/* @flow */

import id from 'lodash/identity';
import React from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import ApplyForm from '../../forms/apply-form/apply-form';

import { submit } from '../../core/modules/apply';

import styles from './apply.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  submit: Function;
}

const ApplyPage = (props: Props): HTMLElement => (
  <div className={styles.apply}>
    <Header
      title="Apply to Sell"
      legend="Apply a merchant request to become a member of BlaBla."
    />
    <ApplyForm onSubmit={props.submit} />
  </div>
);

export default connect(id, { submit })(ApplyPage);
