
import classNames from 'classnames';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton } from '../common/buttons';
import ContextBox from './content-box';

const renderActions = props => {
  return props.isEditing ? null : <EditButton onClick={props.editAction} />;
};

const renderFooter = props => {
  if (props.isEditing) {
    return (
      <footer>
        <PrimaryButton onClick={props.doneAction}>
          Done
        </PrimaryButton>
      </footer>
    );
  } else {
    return null;
  }
};

const EditableContentBox = props => {
  return (
    <ContextBox actionBlock={ renderActions(props) } footer={ renderFooter(props) } isTable={true} {...props}>
      {props.renderContent(props.isEditing, props)}
    </ContextBox>
  );
};

EditableContentBox.propTypes = {
  className: PropTypes.string,
  editContent: PropTypes.node,
  viewContent: PropTypes.node,
  isEditing: PropTypes.bool,
  editAction: PropTypes.func,
  doneAction: PropTypes.func,
  renderContent: PropTypes.func
};

EditableContentBox.defaultProps = {
  renderContent: (isEditing, props) => {
    return isEditing ? props.editContent : props.viewContent;
  }
};

export default EditableContentBox;
