// @flow

// libs
import classNames from 'classnames';
import React, { Component } from 'react';
import { connect } from 'react-redux';

// components
import NotificationIndicator from './indicator';
import NotificationPanel from './panel';

// redux
import * as NotificationActions from 'modules/activity-notifications';

// styles
import s from './block.css';

type Props = {
  count?: number;
  displayed?: boolean;
  notifications?: Array<*>;
  className?: string;
  markAsReadAndClose: Function;
  markAsRead: Function;
  toggleNotifications: Function;
  startFetchingNotifications: Function;
};

class NotificationBlock extends Component {

  props: Props;

  _block: HTMLElement;

  componentDidMount() {
    this.props.startFetchingNotifications();
    window.addEventListener('click', this.handleClickOutside, true);
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.handleClickOutside, true);
  }

  handleClickOutside = (e) => {
    if (this._block && !this._block.contains(e.target) && this.props.displayed) {
      this.props.markAsReadAndClose();
    }
  }

  render() {
    return (
      <div className={classNames(s.block, this.props.className)} ref={c => this._block = c}>
        <NotificationIndicator
          count={this.props.count}
          displayed={this.props.displayed}
          markAsReadAndClose={this.props.markAsReadAndClose}
          toggleNotifications={this.props.toggleNotifications}
        />
        <NotificationPanel
          className={s.popup}
          displayed={this.props.displayed}
          notifications={this.props.notifications}
          markAsRead={this.props.markAsRead}
          markAsReadAndClose={this.props.markAsReadAndClose}
        />
      </div>
    );
  }
}

export default connect(state => state.activityNotifications, NotificationActions)(NotificationBlock);
