// @flow
import React from 'react';
import styles from './breadcrumb.css';

import { Link } from 'react-router';

type Props = {
  title: string
}

const Breadcrumb = (props: Props) => {
  return (
    <div styleName="navigation">
      <Link styleName="nav-link" to="/profile">My Account</Link>
      <div styleName="nav-sep">{'>'}</div>
      <div styleName="nav-item">{props.title}</div>
    </div>
  );
};

export default Breadcrumb;
