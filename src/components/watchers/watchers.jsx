import React, { PropTypes } from 'react';
import Panel from '../panel/panel';
import { AddButton } from '../common/buttons';

export default class Watchers extends React.Component {

  static propTypes = {
    assignees: PropTypes.array,
    watchers: PropTypes.array
  };

  get assignees() {
    return (
      <div className="fc-watchers__empty-list fc-watchers__assignees-empty">
        Unassigned
      </div>
    );
  }

  get watchers() {
    return (
      <div className="fc-watchers__empty-list fc-watchers__watchers-empty">
        Unwatched
      </div>
    );
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
