/* @flow */

import cx from 'classnames';
import React from 'react';

import InkBar from '../ink-bar/ink-bar';

import styles from './steps.css';

import type { HTMLElement } from '../../core/types';

export type Step = {
  key: string;
  title: string;
  active: boolean;
}

type Props = {
  steps: Array<Step>;
}

const renderStep = (step: Step) => {
  const cls = cx(styles.step, {
    [styles.active]: step.active,
  });

  return (
    <span className={cls} key={step.key}>{step.title}</span>
  );
};

const Steps = ({ steps }: Props): HTMLElement => {
  const inkWidth = 100 / steps.length;
  const inkPosition = 100 * steps.findIndex(step => step.active);

  return (
    <div className={styles.steps}>
      {steps.map(renderStep)}
      <InkBar width={`${inkWidth}%`} left={`${inkPosition}%`} />
    </div>
  );
};

export default Steps;
