
import classNames from 'classnames';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton } from '../common/buttons';
import ContextBox from './content-box';

export const EditDoneButton = props => {
  return (
    <PrimaryButton onClick={props.doneAction} {...props}>
      {props.children || 'Done'}
    </PrimaryButton>
  );
};

EditDoneButton.propTypes = {
  doneAction: PropTypes.func,
  children: PropTypes.node
};

const EditableContentBox = props => {
  const {editFooter, indentContent, ...rest} = props;

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
        { props.renderContent(props.isEditing, props) }
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
  editAction: PropTypes.func,
  doneAction: PropTypes.func,
  editFooter: PropTypes.node,
  renderContent: PropTypes.func,
  renderFooter: PropTypes.func,
  renderActions: PropTypes.func,
  title: PropTypes.string,
  indentContent: PropTypes.bool
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
    return props.isEditing ? null : <EditButton onClick={props.editAction} />;
  }
};

export default EditableContentBox;
