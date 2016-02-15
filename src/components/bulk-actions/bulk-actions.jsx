// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import * as bulkActions from '../../modules/bulk';

// components
import SuccessNotification from '../bulk-actions/success-notification';
import ErrorNotification from '../bulk-actions/error-notification';


const mapStateToProps = (state, {module}) => {
  return {
    bulk: state[module].bulk,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class BulkActions extends React.Component {
  static propTypes = {
    module: PropTypes.string.isRequired,
    actions: PropTypes.arrayOf(PropTypes.array).isRequired,
    entityForms: PropTypes.arrayOf(PropTypes.string).isRequired,
    renderDetail: PropTypes.func.isRequired,
    bulk: PropTypes.shape({
      successes: PropTypes.object,
      errors: PropTypes.object,
      messages: PropTypes.shape({
        success: PropTypes.string,
        error: PropTypes.string,
      }),
    }).isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
    children: PropTypes.element.isRequired,
  };

  state = {
    modal: null,
  };

  get bulkMessages() {
    const {entityForms, renderDetail} = this.props;
    const {successes, errors, messages} = this.props.bulk;
    const {clearSuccesses, clearErrors} = this.props.bulkActions;

    const notifications = [];

    if (successes) {
      notifications.push(
        <SuccessNotification key="successes"
                             entityForms={entityForms}
                             overviewMessage={messages.success}
                             onHide={clearSuccesses}>
          {_.map(successes, renderDetail)}
        </SuccessNotification>
      );
    }

    if (errors) {
      notifications.push(
        <ErrorNotification key="errors"
                           entityForms={entityForms}
                           overviewMessage={messages.error}
                           onHide={clearErrors}>
          {_.map(errors, renderDetail)}
        </ErrorNotification>
      );
    }

    return notifications;
  }

  @autobind
  getActionWrapper([label, handler, successMessage, errorMessage]) {
    const {entityForms} = this.props;

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
          entityForms,
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
        {this.bulkMessages}
        {this.child}
        {this.state.modal}
      </div>
    );
  }
}
