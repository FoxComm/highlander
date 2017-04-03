// @flow
// Wrapper for input which add small loading animation to the end of input.

import React, { Element } from 'react';
import s from './loading-input-wrapper.css';

import WaitAnimation from '../common/wait-animation';

type Props = {
  children?: Element<*>,
  inProgress: boolean,
};

const LoadingInputWrapper = (props: Props) => {
  const animation = props.inProgress ? <WaitAnimation className={s.loader} size="s" /> : null;

  return (
    <div className={s.wrapper}>
      {props.children}
      {animation}
    </div>
  );
};

export default LoadingInputWrapper;
