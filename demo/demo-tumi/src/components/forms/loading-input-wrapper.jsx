// @flow
// Wrapper for input which add small loading animation to the end of input.

// libs
import classNames from 'classnames';
import React from 'react';

// styles
import s from './loading-input-wrapper.css';

import WaitAnimation from 'ui/wait-animation/wait-animation';

type Props = {
  children?: Element<*>,
  inProgress: boolean,
  className?: string,
  animationSize?: number,
};

const LoadingInputWrapper = (props: Props) => {
  const { animationSize = 's' } = props;
  const animation = props.inProgress ? <WaitAnimation size={animationSize} /> : null;

  return (
    <div className={classNames(s.wrapper, props.className)}>
      {props.children}
      <div className={s.loadingWrapper}>{animation}</div>
    </div>
  );
};

export default LoadingInputWrapper;
