// @flow
// Wrapper for input which add small loading animation to the end of input.

// libs
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './loading-input-wrapper.css';

import Spinner from 'components/core/spinner';

type Props = {
  children?: Element<*>,
  inProgress: boolean,
  className?: string,
};

const LoadingInputWrapper = (props: Props) => {
  const animation = props.inProgress ? <Spinner className={s.spinner} size="s" /> : null;

  return (
    <div className={classNames(s.wrapper, props.className)}>
      {props.children}
      {animation}
    </div>
  );
};

export default LoadingInputWrapper;
