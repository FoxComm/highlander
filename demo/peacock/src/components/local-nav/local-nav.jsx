// @flow

import React from 'react';
import { Link } from 'react-router';

import styles from './local-nav.css';

type LinkItem = {
  label: string,
  to: string,
};

type Props = {
  categoryName: string,
  links: Array<LinkItem>,
};

const LocalNav = (props: Props) => {
  const items = props.links.map((linkData: LinkItem) => {
    return (
      <li styleName="local-nav-item" key={linkData.to}>
        <Link to={linkData.to}>
          {linkData.label}
        </Link>
      </li>
    );
  });

  return (
    <div styleName="local-nav">
      <div styleName="local-nav-header">
        {props.categoryName}
      </div>
      <ul styleName="local-nav-items">
        {items}
      </ul>
    </div>
  );
};

export default LocalNav;
