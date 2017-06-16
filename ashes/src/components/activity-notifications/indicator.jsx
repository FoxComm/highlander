// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import Icon from 'components/core/icon';

// styles
import s from './indicator.css';

// types
type Props = {
  /** The number of available notifications */
  count?: number;
  /** If true, shows popup with a notifications list */
  displayed?: boolean;
  /** A callback, which supposed to switch `displayed` shmewhere outside the component */
  toggleNotifications: Function;
  /** A callback, which supposed to mark all notifications as read outside the component */
  markAsReadAndClose: Function;
};

export default class NotificationIndicator extends Component {

  props: Props;

  get indicator() {
    if (!this.props.count) {
      return null;
    }

    let count = String(this.props.count);

    if (Number(this.props.count) > 99) {
      count = '99+';
    }

    return (
      <div className={s.indicator} key={count}>
        {count}
      </div>
    );
  }

  @autobind
  toggleNotifications() {
    if (this.props.displayed) {
      this.props.markAsReadAndClose();
    } else {
      this.props.toggleNotifications();
    }
  }

  render() {
    const classes = classNames(s.toggle, {
      [s.active]: this.props.displayed
    });

    return (
      <div className={s.block}>
        <div
          className={classes}
          onClick={this.toggleNotifications}
        >
          <Icon name="bell" />
          {this.indicator}
        </div>
      </div>
    );
  }
}
