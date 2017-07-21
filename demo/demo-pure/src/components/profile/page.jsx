// @flow
import React, { Element } from 'react';

import styles from './page.css';

type Props = {
  children: Element<*>,
}

const Page = (props: Props) => {
  return (
    <div styleName="profile">
      <h1 styleName="title">My Account</h1>
      <div styleName="content">
        {props.children}
      </div>
    </div>
  );
};

export default Page;
