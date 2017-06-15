// libs
import { get, map } from 'lodash';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

//helpers
import { getStore } from 'lib/store-creator';

// components
import SuccessNotification from '../bulk-actions/success-notification';
import ErrorNotification from '../bulk-actions/error-notification';
import ErrorAlerts from '../alerts/error-alerts';


type Props = {
  storePath: string,
  bulkModule?: string,
  module: string,
  entity: string,
  renderDetail: () => ReactElement,
  hideAlertDetails?: boolean,
  className?: string,
  bulk: {
    successes: ?Object,
    errors: ?Object,
    messages: ?{
      success: string,
      error: string,
    },
  },
  bulkActions: Object,
};


class BulkMessages extends Component {
  props: Props;

  componentWillUnmount() {
    this.props.bulkActions.clearSuccesses();
    this.props.bulkActions.clearErrors();
  }

  get className() {
    const { className } = this.props;
    if (className == null) return 'fc-bulk-messages';

    return `fc-bulk-messages ${className}`;
  }

  render() {
    const { bulk, bulkActions, entity, renderDetail, hideAlertDetails } = this.props;
    const { successes, errors, messages, error } = bulk;
    const { clearSuccesses, clearErrors, clearError } = bulkActions;
    const notifications = [];

    if (successes) {
      notifications.push(
        <SuccessNotification key="successes"
                             entity={entity}
                             hideAlertDetails={hideAlertDetails}
                             overviewMessage={messages.success}
                             onHide={clearSuccesses}>
          {map(successes, renderDetail)}
        </SuccessNotification>
      );
    }

    if (errors) {
      notifications.push(
        <ErrorNotification key="errors"
                           entity={entity}
                           overviewMessage={messages.error}
                           hideAlertDetails={hideAlertDetails}
                           onHide={clearErrors}>
          {map(errors, renderDetail)}
        </ErrorNotification>
      );
    }

    if (error) {
      notifications.push(
        <ErrorAlerts key="general-error" error={error} closeAction={clearError} />
      );
    }

    return (
      <div className={this.className}>
        {notifications}
      </div>
    );
  }
}

const mapState = (state, { storePath }) => ({
  bulk: get(state, storePath, {}),
});

const mapActions = (dispatch, { bulkModule, module }) => {
  const { actions } = bulkModule ? getStore(bulkModule) : getStore(`${module}.bulk`);
  return {
    bulkActions: bindActionCreators(actions, dispatch),
  };
};

export default connect(mapState, mapActions)(BulkMessages);
