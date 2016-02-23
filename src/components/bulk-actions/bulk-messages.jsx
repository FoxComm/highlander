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


const mapStateToProps = (state, {storePath}) => {
  return {
    bulk: _.get(state, storePath, {}),
  };
};

const mapDispatchToProps = (dispatch, {module}) => {
  const {actions} = getStore('bulk', module);

  return {
    bulkActions: bindActionCreators(actions, dispatch),
  };
};

const BulkMessages = ({bulk, bulkActions, entity, renderDetail}) => {
  const {successes, errors, messages} = bulk;
  const {clearSuccesses, clearErrors} = bulkActions;

  const notifications = [];

  if (successes) {
    notifications.push(
      <SuccessNotification key="successes"
                           entity={entity}
                           overviewMessage={messages.success}
                           onHide={clearSuccesses}>
        {_.map(successes, renderDetail)}
      </SuccessNotification>
    );
  }

  if (errors) {
    notifications.push(
      <ErrorNotification key="errors"
                         entity={entity}
                         overviewMessage={messages.error}
                         onHide={clearErrors}>
        {_.map(errors, renderDetail)}
      </ErrorNotification>
    );
  }

  return (<div>{notifications}</div>);
};

BulkMessages.propTypes = {
  storePath: PropTypes.string.isRequired,
  module: PropTypes.string.isRequired,
  entity: PropTypes.string.isRequired,
  renderDetail: PropTypes.func.isRequired,

  //computed
  bulk: PropTypes.shape({
    successes: PropTypes.object,
    errors: PropTypes.object,
    messages: PropTypes.shape({
      success: PropTypes.string,
      error: PropTypes.string,
    }),
  }).isRequired,
  bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
};

export default connect(mapStateToProps, mapDispatchToProps)(BulkMessages);
