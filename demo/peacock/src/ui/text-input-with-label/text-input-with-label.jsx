// @flow

import React from 'react';
import styles from './text-input-with-label.css';
import TextInput from '../text-input/text-input';

type Props = {
  [key: string]: any,
};

const TextInputWithLabel = (props: Props) => {
  const {label, children, ...rest} = props;

  const content = children || <TextInput {...rest} />;

  return (
    <div styleName="root">
      {content}
      {label && <span styleName="label">{label}</span>}
    </div>
  );
};

export default TextInputWithLabel;
