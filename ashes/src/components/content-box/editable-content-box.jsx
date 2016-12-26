
import classNames from 'classnames';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton } from '../common/buttons';
import ContextBox from './content-box';

export const EditDoneButton = props => {
  const { doneAction, ...rest } = props;
  return (
    <PrimaryButton onClick={doneAction} {...rest}>
      {props.children || 'Done'}
    </PrimaryButton>
  );
};

EditDoneButton.propTypes = {
  doneAction: PropTypes.func,
  children: PropTypes.node
};

const EditableContentBox = props => {
  const {editFooter, indentContent, renderContent, ...rest} = props;

  const className = classNames(
    'fc-editable-content-box',
    props.className, {
      'is-editing': props.isEditing,
      'is-indent-for-content': indentContent
    }
  );

  return (
    <ContextBox
      actionBlock={ props.renderActions(props) }
      {...rest}
      className={ className }
      indentContent={false}
    >
      <div className="fc-editable-content-box-content">
        { renderContent(props.isEditing, props) }
      </div>
      { props.renderFooter && props.renderFooter(rest, editFooter) }
    </ContextBox>
  );
};

EditableContentBox.propTypes = {
  className: PropTypes.string,
  editContent: PropTypes.node,
  viewContent: PropTypes.node,
  isEditing: PropTypes.bool,
  editButtonId: PropTypes.string,
  editAction: PropTypes.func,
  doneAction: PropTypes.func,
  editingActions: PropTypes.node,
  editFooter: PropTypes.node,
  renderContent: PropTypes.func,
  renderFooter: PropTypes.func,
  renderActions: PropTypes.func,
  title: PropTypes.node,
  indentContent: PropTypes.bool,
};

EditableContentBox.defaultProps = {
  editingActions: null,
  editButtonId: null,
};

// eslint you are drunk, renderFooter and renderActions are just functions
/*eslint "react/prop-types": 0*/

EditableContentBox.defaultProps = {
  renderContent: (isEditing, props) => {
    return isEditing ? props.editContent : props.viewContent;
  },
  renderFooter: (props, footer) => {
    if (props.isEditing) {
      return (
        <footer className="fc-editable-content-box-footer">
          {footer}
          <EditDoneButton doneAction={props.doneAction} />
        </footer>
      );
    } else {
      return null;
    }
  },
  renderActions: props => {
    if (props.isEditing) {
      return props.editingActions;
    } else {
      return props.editAction
        ? <EditButton id={props.editButtonId} onClick={props.editAction} />
        : <EditButton id={props.editButtonId} disabled={true} />;
    }
  },
};

export default EditableContentBox;
