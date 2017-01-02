import React, { PropTypes } from 'react';
import Initials from '../user-initials/initials';
import DetailedInitials from '../user-initials/detailed-initials';
import { EditButton, DeleteButton } from '../common/buttons';

const NoteControls = props => {
  return (
    <div className="fc-notes-item-controls">
      <DetailedInitials {...props.model.author}/>
      <DeleteButton onClick={() => props.onDeleteClick(props.model)} />
      <EditButton onClick={() => props.onEditClick(props.model)} />
    </div>
  );
};

NoteControls.propTypes = {
  model: PropTypes.shape({
    author: PropTypes.object.isRequired
  }).isRequired,
  onEditClick: PropTypes.func.isRequired,
  onDeleteClick: PropTypes.func.isRequired
};

export default NoteControls;
