// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import { Button } from 'components/core/button';

// styles
import s from './indicator.css';

// types
type Props = {
  count?: number;
  displayed?: boolean;
  toggleNotifications: Function;
  markAsReadAndClose: Function;
};

export default class NotificationIndicator extends Component {

  props: Props;

  get indicator() {
    let count = String(this.props.count);

    if (this.props.count != null && this.props.count > 99) {
      count = '99+';
    }

    if (this.props.count) {
      return (
        <div className={s.indicator} key={count}>
          {count}
        </div>
      );
    }
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
        <Button
          icon="bell"
          className={classes}
          onClick={this.toggleNotifications}
          fullWidth
        >
          {this.indicator}
        </Button>
      </div>
    );
  }
}
