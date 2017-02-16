// @flow

import React, { Element} from 'react';
import styles from './css/wrap-to-lines.css';

type Props = {
  children?: Element<*>,
  className?: string
}

const WrapToLines = (props: Props) => {
  const {className, children, ...rest} = props;
  return (
    <div styleName="block" className={className} {...rest}>
      <div styleName="line"></div>
      <div styleName="content">{children}</div>
      <div styleName="line"></div>
    </div>
  );
};


export default WrapToLines;
