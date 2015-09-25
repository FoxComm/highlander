'use strict';

import React from 'react';
import UserInitials from '../users/initials';

export default class NotesItemControls extends React.Component {
  onEditClick() {
    this.props.onEditClick(this.props.model);
  }

  onDeleteClick() {
    this.props.onDeleteClick(this.props.model);
  }

  render() {
    return (
      <div className="fc-notes-item-controls">
        <UserInitials model={this.props.model.author}/>
        <button onClick={this.onDeleteClick.bind(this)}>
          <i className="icon-trash"/>
        </button>
        <button onClick={this.onEditClick.bind(this)}>
          <i className="icon-edit"/>
        </button>
      </div>
    );
  }
}

NotesItemControls.propTypes = {
  model: React.PropTypes.object,
  onEditClick: React.PropTypes.func,
  onDeleteClick: React.PropTypes.func
};
