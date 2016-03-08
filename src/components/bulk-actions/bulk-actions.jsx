// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import * as bulkActions from '../../modules/bulk';

//helpers
import { getStore } from '../../lib/store-creator';

// components
import SuccessNotification from '../bulk-actions/success-notification';
import ErrorNotification from '../bulk-actions/error-notification';


const mapDispatchToProps = (dispatch, {module}) => {
  const {actions} = getStore('bulk', module);

  return {
    bulkActions: bindActionCreators(actions, dispatch),
  };
};

@connect(null, mapDispatchToProps)
export default class BulkActions extends React.Component {
  static propTypes = {
    module: PropTypes.string.isRequired,
    entity: PropTypes.string.isRequired,
    actions: PropTypes.arrayOf(PropTypes.array).isRequired,
    children: PropTypes.element.isRequired,

    //computed
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
  };

  state = {
    modal: null,
  };

  @autobind
  getActionWrapper([label, handler, successMessage, errorMessage]) {
    const {module, entity} = this.props;

    const setState = this.setState.bind(this);

    return [
      label,
      (allChecked, toggledIds) => {
        //get result of original handler
        const result = handler(allChecked, toggledIds);

        //returned result is suggested to be a modal - nothing to be done more otherwise
        if (!result) {
          return;
        }

        //result is react component, and we are suggesting, it's a modal
        //modal's valuable props are provided and onCancel/onConfirm are wrapped
        const modal = React.cloneElement(result, {
          isVisible: true,
          module,
          entity,
          onCancel: this.getModalCancelHandler(result.props.onCancel),
          onConfirm: this.getModalConfirmHandler(result.props.onConfirm, {
            success: successMessage,
            error: errorMessage,
          }),
        });

        setState({modal});
      },
    ];
  }

  @autobind
  hideModal() {
    this.setState({modal: null});
  }

  @autobind
  getModalCancelHandler(onCancel) {
    return (...args) => {
      this.hideModal();
      (onCancel || _.noop)(...args);
    };
  }

  @autobind
  getModalConfirmHandler(onConfirm, messages) {
    const {reset, setMessages} = this.props.bulkActions;

    return (...args) => {
      reset();
      setMessages(messages);
      this.hideModal();
      (onConfirm || _.noop)(...args);
    };
  }

  get child() {
    const {actions, children} = this.props;

    return React.cloneElement(children, {
      bulkActions: actions.map(this.getActionWrapper),
    });
  }

  render() {
    return (
      <div>
        {this.child}
        {this.state.modal}
      </div>
    );
  }
}
