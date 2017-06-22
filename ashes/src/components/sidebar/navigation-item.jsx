/* @flow */

import _ from 'lodash';
import React from 'react';
import classNames from 'classnames';

import { IndexLink } from 'components/link';
import type { Claims } from 'lib/claims';

import SvgIcon from 'components/core/svg-icon';

import styles from './navigation-item.css';

type Props = {
  to: string,
  icon: string,
  title: string,
  forceActive?: boolean,
  routes: Array<Object>,
  actualClaims: Claims | string,
  expectedClaims: Claims | string,
  linkParams?: any,
};

const NavigationItem = (props: Props) => {
  const containerClass = (): string => {
    const routeNames = props.routes.map(route => route.name);
    let isActive;

    if (props.forceActive !== void 0) {
      isActive = props.forceActive;
    } else {
      isActive = _.includes(routeNames, props.to) || _.includes(routeNames, `${props.to}-base`);
    }

    /*
     If I'm in customer groups, routeNames contains customers-base,
     thus setting isActive to true for both 'Customers' and 'Customer Groups'
     menu items
     */
    if (props.to === 'customers' && (_.includes(routeNames, 'groups') || _.includes(routeNames, 'groups-base'))) {
      isActive = false;
    }

    return classNames('fc-navigation-item-container', {
      _active: isActive,
    });
  };

  return (
    <div className={containerClass()}>
      <div className="fc-navigation-item">
        <IndexLink
          to={props.to}
          params={props.linkParams || {}}
          className="fc-navigation-item__link"
          actualClaims={props.actualClaims}
          expectedClaims={props.expectedClaims}
        >
          <SvgIcon name={props.icon} styleName="nav-item" />
          <span>{props.title}</span>
        </IndexLink>
      </div>
    </div>
  );
};

export default NavigationItem;
