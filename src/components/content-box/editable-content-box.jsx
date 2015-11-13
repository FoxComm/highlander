
import classNames from 'classnames';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton } from '../common/buttons';
import ContextBox from './content-box';

export const EditDoneButton = props => {
  return (
    <PrimaryButton onClick={props.doneAction} {...props}>
      {props.children || "Done"}
    </PrimaryButton>
  );
};

EditDoneButton.propTypes = {
  doneAction: PropTypes.func
};

const EditableContentBox = props => {
  const {editFooter, ...rest} = props;

  return (
    <ContextBox
      actionBlock={ props.renderActions(props) }
      footer={ props.renderFooter && props.renderFooter(rest, editFooter) }
      isTable={true}
      {...rest}
    >
      {props.renderContent(props.isEditing, props)}
      {props.renderActionsInBody(props)}
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
  editFooter: PropTypes.func,
  renderContent: PropTypes.func,
  renderFooter: PropTypes.func,
  renderActions: PropTypes.func,
  renderActionsInBody: PropTypes.func
};

EditableContentBox.defaultProps = {
  renderContent: (isEditing, props) => {
    return isEditing ? props.editContent : props.viewContent;
  },
  renderFooter: (props, footer) => {
    if (props.isEditing) {
      return (
        <footer>
          {footer}
          <EditDoneButton doneAction={props.doneAction} />
        </footer>
      );
    } else {
      return null;
    }
  },
  renderActions: props => {
    return props.isEditing ? null : <EditButton onClick={props.editAction} />;
  },
  renderActionsInBody: ({isTable = true, ...props}) => {
    if (!isTable && props.doneAction && !props.renderFooter && props.isEditing) {
      return (
        <div>
          <EditDoneButton doneAction={props.doneAction} />
        </div>
      );
    }
  }
};

export default EditableContentBox;
