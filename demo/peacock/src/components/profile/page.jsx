// @flow
import React, { Element } from 'react';

import styles from './page.css';

type Props = {
  children: Element<*>,
}

const Page = (props: Props) => {
  return (
    <div styleName="profile">
      <div styleName="content">
        {props.children}
      </div>
    </div>
  );
};

export default Page;
