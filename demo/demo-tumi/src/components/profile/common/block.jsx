// @flow
import React, { Element } from 'react';

import styles from './block.css';

type BlockProps = {
  title: string,
  children?: Element<*>|Array<Element<*>>,
}

const Block = (props: BlockProps) => {
  return (
    <div styleName="block">
      <div styleName="header">{props.title}</div>
      <div styleName="content">{props.children}</div>
    </div>
  );
};

export default Block;
