'use strict';

import React, { PropTypes } from 'react';
import UserInitials from '../users/initials';
import { DefaultButton } from '../common/buttons';

const NoteControls = (props) => {
  return (
    <div className="fc-notes-item-controls">
      <UserInitials model={props.model.author}/>
      <DefaultButton icon="trash" onClick={() => props.onDeleteClick(props.model)} />
      <DefaultButton icon="edit" onClick={() => props.onEditClick(props.model)} />
    </div>
  );
};

NoteControls.propTypes = {
  model: PropTypes.shape({
    author: PropTypes.string
  }).isRequired,
  onEditClick: PropTypes.func.isRequired,
  onDeleteClick: PropTypes.func.isRequired
};

export default NoteControls;
