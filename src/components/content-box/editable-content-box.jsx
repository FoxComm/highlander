'use strict';

import React from 'react';
import {EditButton, PrimaryButton} from '../common/buttons';

const EditableContentBox = (props) => {
  const compositeClassName = `fc-content-box ${props.className}`;
  const content = props.isEditing ? props.editContent : props.viewContent;
  return (
    <div className={compositeClassName}>
      {renderTitle(props)}
      {content}
      {renderFooter(props)}
    </div>
  );
};

const renderTitle = (props) => {
  let editButton = null;
  if (!props.isEditing) {
    editButton = <EditButton onClick={props.editAction} />;
  }

  return (
    <header>
      <div className='fc-grid fc-content-box-header'>
        <div className='fc-col-md-2-3 fc-title'>{props.title}</div>
        <div className='fc-col-md-1-3 fc-controls'>{editButton}</div>
      </div>
    </header>
  );
};

const renderFooter = (props) => {
  if (props.isEditing) {
    return (
      <footer>
        <PrimaryButton onClick={props.doneAction}>
          Done
        </PrimaryButton>
      </footer>
    );
  } else {
    return <div></div>;
  }
};

export default EditableContentBox;
