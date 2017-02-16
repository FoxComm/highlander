/* @flow */

import _ from 'lodash';
import React, { Element} from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { getStore } from '../../lib/store-creator';

import { ChangeStateModal } from '../bulk-actions/modal';
import SchedulerModal from './scheduler-modal';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { Link } from '../link';

type RefId = string|number;

type Props = {
  entity: string;
  bulkActions: {
    changeState: (ids: Array<RefId>, isActivation: boolean) => void;
    updateAttributes: (ids: Array<RefId>, attributes: Attributes) => void;
  };
  children: Element<*>;
};

const mapDispatchToProps = (dispatch: Function, { entity }) => {
  const module = `${entity}s`;

  const {actions} = getStore(`${module}.bulk`);
  return {
    bulkActions: bindActionCreators(actions, dispatch),
  };
};

const changeStateHandler = function(props: Props, isActivation: boolean): Function {
  const stateTitle = isActivation ? 'Active' : 'Inactive';

  return (allChecked, toggledIds) => {
    const {changeState} = props.bulkActions;

    return (
      <ChangeStateModal
        count={toggledIds.length}
        stateTitle={stateTitle}
        onConfirm={() => changeState(toggledIds, isActivation)}
      />
    );
  };
};

const scheduleHandler = (props: Props) => (allChecked, toggledIds) => {
  const {updateAttributes} = props.bulkActions;

  const handleConfirm = (attributes: Attributes) => {
    updateAttributes(toggledIds, attributes);
  };

  return (
    <SchedulerModal
      entity={props.entity}
      count={toggledIds.length}
      onConfirm={handleConfirm}
    />
  );
};

const renderDetail = (props: Props) => (messages, id) => {
  const idParam = `${props.entity}Id`;
  const body = _.isEmpty(messages) ? null : [': ', messages];
  const entityCap = _.capitalize(props.entity);

  return (
    <span key={id}>
        {entityCap} <Link to={`${props.entity}-details`} params={{[idParam]: id}}>{id}</Link>{body}
      </span>
  );
};

const BulkWrapper = (props: Props) => {
  const { entity } = props;
  const module = `${entity}s`;

  const bulkActions = [
    ['Activate', changeStateHandler(props, true), 'successfully activated', 'could not be activated'],
    ['Deactivate', changeStateHandler(props, false), 'successfully deactivated', 'could not be deactivated'],
    [`Schedule ${entity}s`, scheduleHandler(props), 'successfully updated', 'could not be updated'],
  ];

  return (
    <div>
      <BulkMessages
        storePath={`${module}.bulk`}
        module={module}
        entity={entity}
        renderDetail={renderDetail(props)} />
      <BulkActions
        module={module}
        entity={entity}
        watchActions={true}
        actions={bulkActions}>
        {props.children}
      </BulkActions>
    </div>
  );
};

export default connect(null, mapDispatchToProps)(BulkWrapper);

