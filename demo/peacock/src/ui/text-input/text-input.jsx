// @flow

import React from 'react';
import s from './text-input.css';
import classNames from 'classnames';

type Props = {
  className?: string,
}

const TextInput = (props: Props) => {
  const adjoins = props.adjoin ? props.adjoin.split('') : [];
  const adjoinClassNames = adjoins.map(side => s[`adjoin-${side}`]);

  const {className, ...rest} = props;

  const cls = classNames(s.textInput, adjoinClassNames, className);

  return <input className={cls} type="text" {...rest} />;
};

export default TextInput;
