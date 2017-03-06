/* @flow */

import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import styles from './navigation-item.css';

import { IndexLink, Link } from '../link';
import type { Claims } from 'lib/claims';

import Icon from '../icon/icon';

type Props = {
    to:             string,
    icon:           string,
    title:          string,
    routes:         Array<Object>,
    actualClaims:   Claims,
    expectedClaims: Claims
  };

export default class NavigationItem extends React.Component {
  props: Props;

  get containerClass(): string {
    const routeNames = this.props.routes.map(route => route.name);
    let isActive = _.includes(routeNames, this.props.to) ||
      _.includes(routeNames, `${this.props.to}-base`);

    /*
       If I'm in customer groups, routeNames contains customers-base,
       thus setting isActive to true for both 'Customers' and 'Customer Groups'
       menu items
    */
    if(this.props.to === 'customers' && (_.includes(routeNames, 'groups') || _.includes(routeNames, 'groups-base') ) )
      isActive = false;
  
    return classNames('fc-navigation-item-container', {
      '_active': isActive
    });
  }

  render() {
    return (
      <div className={this.containerClass}>
        <div className="fc-navigation-item">
          <IndexLink
            to={this.props.to}
            className="fc-navigation-item__link"
            actualClaims={this.props.actualClaims}
            expectedClaims={this.props.expectedClaims}>
            <Icon name={this.props.icon} className={styles["nav-item"]} />
            <span>{this.props.title}</span>
          </IndexLink>
        </div>
      </div>
    );
  }
}
