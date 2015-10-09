'use strict';

import React from 'react';
import UserInitials from '../users/initials';

export default class NoteControls extends React.Component {
  static propTypes = {
    model: React.PropTypes.object,
    onEditClick: React.PropTypes.func,
    onDeleteClick: React.PropTypes.func
  };

  render() {
    return (
      <div className="fc-notes-item-controls">
        <UserInitials model={this.props.model.author}/>
        <button className="fc-btn" onClick={this.props.onDeleteClick.bind(this, this.props.model)}>
          <i className="icon-trash"/>
        </button>
        <button className="fc-btn" onClick={this.props.onEditClick.bind(this, this.props.model)}>
          <i className="icon-edit"/>
        </button>
      </div>
    );
  }
}
