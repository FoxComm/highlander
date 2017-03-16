// @flow

import React from 'react';
import styles from './profile.css';

import Breadcrumb from './breadcrumb';

import type { HTMLElement } from 'types';

type Route = {
  component: {
    title?: string,
  },
}

type Props = {
  children: HTMLElement|Array<HTMLElement>,
  routes: Array<Route>,
  params: Object,
}

const ProfileUnit = (props: Props) => {
  const lastRoute = props.routes[props.routes.length - 1];
  let title = lastRoute.component.title;
  if (typeof title == 'function') {
    title = title(props.params);
  }
  return (
    <div styleName="profile">
      <Breadcrumb title={title} />
      {props.children}
    </div>
  );
};

export default ProfileUnit;
