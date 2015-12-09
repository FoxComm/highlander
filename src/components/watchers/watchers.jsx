import React, { PropTypes } from 'react';
import Panel from '../panel/panel';
import { AddButton } from '../common/buttons';

export default class Watchers extends React.Component {

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
          <div className="fc-watchers__users-row">
            <div className="fc-watchers__add-control">
              <AddButton className="fc-watchers__add-button"/>
            </div>
          </div>
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Watchers
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" href="#">watch</a>
            </div>
          </div>
          <div className="fc-watchers__users-row">
            <AddButton className="fc-watchers__add-button"/>
          </div>
        </div>
      </Panel>
    );
  }

}
