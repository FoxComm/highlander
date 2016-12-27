// libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { capitalize } from 'fleck';
import { numberize } from 'lib/text-utils';

//helpers
import { groups } from 'paragons/watcher';
import { getStore } from 'lib/store-creator';

import SelectUsersModal from '../users/select-modal';

const mapDispatchToProps = (dispatch, {module}) => {
  const {actions} = getStore(`${module}.bulk`);

  return {
    bulkActions: bindActionCreators(actions, dispatch),
  };
};

@connect(void 0, mapDispatchToProps)
export default class BulkActions extends Component {
  static propTypes = {
    module: PropTypes.string.isRequired,
    entity: PropTypes.string.isRequired,
    actions: PropTypes.arrayOf(PropTypes.array).isRequired,
    children: PropTypes.element.isRequired,
    watchActions: PropTypes.bool,

    //computed
    selectedWatchers: PropTypes.array.isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
  };

  state = {
    modal: null,
  };

  getWatchModal(group, action, isDirectAction) {
    return (allChecked, toggledIds) => {
      const {bulkActions, entity} = this.props;
      const count = toggledIds.length;
      let label = null;

      if (group === 'watchers') {
        label = isDirectAction ? 'Watchers for' : 'Remove watchers for';
      } else {
        label = isDirectAction ? 'Assign' : 'Unassign';
      }
      const entityForm = numberize(entity, count);
      const bulkAction = isDirectAction ? bulkActions.watch : bulkActions.unwatch;
      const actionForm = _.capitalize(action);

      return (
        <SelectUsersModal
          title={`${actionForm} ${_.capitalize(entityForm)}`}
          bodyLabel={<span>{label} <strong>{count}</strong> {entityForm}</span>}
          saveLabel={group == 'watchers' ? 'Watch' : 'Assign'}
          maxUsers={1}
          onConfirm={users => bulkAction(entity, group, toggledIds, _.map(users, user => user.id))}
        />
      );
    };
  }

  getWatchAction(group, action, isDirectAction, successMessage, errorMessage) {
    const actionForm = isDirectAction ? action : `un${action}`;

    const entityTitle = capitalize(this.props.module);

    return [
      `${_.capitalize(actionForm)} ${entityTitle}`,
      this.getWatchModal(group, actionForm, isDirectAction),
      successMessage,
      errorMessage,
    ];
  }

  @autobind
  getActionWrapper([label, handler, successMessage, errorMessage]) {
    const {module, entity} = this.props;

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

        this.setState({modal});
      },
    ];
  }

  @autobind
  hideModal() {
    this.setState({modal: null});
  }

  @autobind
  getModalCancelHandler(onCancel = _.noop) {
    return (...args) => {
      this.hideModal();
      onCancel(...args);
    };
  }

  @autobind
  getModalConfirmHandler(onConfirm = _.noop, messages) {
    const {reset, setMessages} = this.props.bulkActions;

    return (...args) => {
      reset();
      setMessages(messages);
      this.hideModal();
      onConfirm(...args);
    };
  }

  get actions() {
    let { actions, watchActions } = this.props;

    if (watchActions) {
      actions = [...actions,
        this.getWatchAction(
          groups.assignees, 'assign', true, 'successfully assigned', 'failed to assign'
        ),
        this.getWatchAction(
          groups.assignees, 'assign', false, 'successfully unassigned', 'failed to unassign'
        ),
        this.getWatchAction(
          groups.watchers, 'watch', true, 'successfully started watching', 'failed to start watching'
        ),
        this.getWatchAction(
          groups.watchers, 'watch', false, 'successfully stopped watching', 'failed to stop watching'
        ),
      ];
    }

    return actions;
  }

  get child() {
    const {children} = this.props;

    return React.cloneElement(children, {
      bulkActions: this.actions.map(this.getActionWrapper),
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
