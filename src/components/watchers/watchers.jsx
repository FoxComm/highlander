import _ from 'lodash';
import React, { PropTypes } from 'react';
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
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'}
    ]
  }

  get assignees() {
    if (_.isEmpty(this.props.assignees)) {
      return (
        <div className="fc-watchers__empty-list fc-watchers__assignees-empty">
          Unassigned
        </div>
      );
    } else {
      return this.props.assignees.map(assignee => {
        return (
          <div className="fc-watchers__assignee-cell">
            <UserInitials name={assignee.name} email={assignee.email}/>
          </div>
        );
      });
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
      return this.props.watchers.map(watcher => {
        return (
          <div className="fc-watchers__watcher-cell">
            <UserInitials name={watcher.name} email={watcher.email}/>
          </div>
        );
      });
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
            <AddButton className="fc-watchers__add-button"/>
            {this.watchers}
          </div>
        </div>
      </Panel>
    );
  }

}
