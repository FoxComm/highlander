import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

import Panel from '../panel/panel';
import { AddButton } from '../common/buttons';
import UserInitials from '../users/initials';

export default class Watchers extends React.Component {

  static propTypes = {
    assignees: PropTypes.array,
    watchers: PropTypes.array
  };

  static defaultProps = {
    assignees: [],
    watchers: [
      {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Donkey Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'}
    ]
  }

  get maxDisplayed() {
    return 7;
  }

  get assignees() {
    if (_.isEmpty(this.props.assignees)) {
      return (
        <div className="fc-watchers__empty-list fc-watchers__assignees-empty">
          Unassigned
        </div>
      );
    } else {
      return this.buildRow(this.props.assignees, 'fc-watchers__assignees-row');
    }
  }

  get watchers() {
    if (_.isEmpty(this.props.watchers)) {
      return (
        <div className="fc-watchers__empty-list fc-watchers__watchers-empty">
          Unwatched
        </div>
      );
    } else {
      return this.buildRow(this.props.watchers, 'fc-watchers__watchers-row');
    }
  }

  renderCell(user) {
    return (
      <div className="fc-watchers__cell">
        <UserInitials name={user.name} email={user.email}/>
      </div>
    );
  }

  buildHiddenRow(hiddenCells) {
    return (
      <div className="fc-watchers__rest-cell">
        <i className="fc-icon icon-list"></i>
        <div className="fc-watchers__rest-block">
          <div className="fc-watchers__users-row">
            {hiddenCells}
          </div>
        </div>
      </div>
    );
  }

  buildRow(users, className) {
    const rowClass = classNames("fc-watchers__users-row", className);
    if (users.length <= this.maxDisplayed) {
      const cells = users.map(watcher => this.renderCell(watcher));
      return (
        <div className={rowClass}>
          <AddButton className="fc-watchers__add-button"/>
          {cells}
        </div>
      );
    } else {
      const displayedWatchers = users.slice(0, this.maxDisplayed - 1);
      const hiddenWatchers = users.slice(this.maxDisplayed - 1);
      const displayedCells = displayedWatchers.map(watcher => this.renderCell(watcher));
      const hiddenCells = hiddenWatchers.map(watcher => this.renderCell(watcher));
      return (
        <div className={rowClass}>
          <AddButton className="fc-watchers__add-button"/>
          {displayedCells}
          {this.buildHiddenRow(hiddenCells)}
        </div>
      );
    }
  }

  render() {
    return (
      <Panel className="fc-watchers">
        <div className="fc-watchers__container">
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Assignees
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" href="#">take it</a>
            </div>
          </div>
          <div className="fc-watchers__users-row fc-watchers__assignees">
            <AddButton className="fc-watchers__add-button"/>
            {this.assignees}
          </div>
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Watchers
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" href="#">watch</a>
            </div>
          </div>
          <div className="fc-watchers__users-row fc-watchers__watchers">
            {this.watchers}
          </div>
        </div>
      </Panel>
    );
  }

}
