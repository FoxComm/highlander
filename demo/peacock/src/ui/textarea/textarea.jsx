// @flow

import React from 'react';
import classnames from 'classnames';
import s from './textarea.css';

type Props = {
  [name: string]: any,
}

const TextArea = (props: Props) => {
  const { className, ...rest } = props;
  return <textarea className={classnames(s.textarea, className)} {...rest} />;
};

export default TextArea;
