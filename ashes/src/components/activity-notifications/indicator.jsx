// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import { Button } from 'components/core/button';

export default class NotificationIndicator extends React.Component {

  static propTypes = {
    count: PropTypes.number,
    displayed: PropTypes.bool,
    toggleNotifications: PropTypes.func.isRequired,
    markAsReadAndClose: PropTypes.func.isRequired
  };

  static defaultProps = {
    count: 0,
    displayed: false
  };

  get indicator() {
    const count = this.props.count;

    if (_.isNumber(count) && count > 0) {
      return (
        <div className="fc-activity-notifications__indicator" key={count}>
          <span>{count}</span>
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
    const classes = classNames('fc-activity-notifications__toggle', {
      '_active': this.props.displayed
    });
    return (
      <div className="fc-activity-notifications">
        <Button
          icon="bell"
          className={ classes }
          onClick={ this.toggleNotifications }
        >
          {this.indicator}
        </Button>
      </div>
    );
  }
}
