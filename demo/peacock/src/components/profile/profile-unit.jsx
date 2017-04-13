// @flow

import React, { Element } from 'react';
import styles from './profile.css';

type Route = {
  component: {
    title: string,
  },
}

type Props = {
  children: Element<*>|Array<Element<*>>,
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
      {props.children}
    </div>
  );
};

export default ProfileUnit;
