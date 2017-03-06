import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import styles from './navigation-item.css';

import { IndexLink, Link } from '../link';

import Icon from '../icon/icon';

export default class NavigationItem extends React.Component {

  static propTypes = {
    to: PropTypes.string.isRequired,
    icon: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    routes: PropTypes.array.isRequired
  };

  get containerClass() {
    const routeNames = this.props.routes.map(route => route.name);
    const isActive = _.includes(routeNames, this.props.to) ||
      _.includes(routeNames, `${this.props.to}-base`);
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
