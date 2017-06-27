/* @flow */

import _ from 'lodash';
import React, { Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { getStore } from '../../lib/store-creator';

import { Link } from 'components/link';
import { ChangeStateModal } from '../bulk-actions/modal';
import { DeleteModal } from '../bulk-actions/modal';
import SchedulerModal from './scheduler-modal';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';

type RefId = string|number;

type Props = {
  entity: string;
  hideAlertDetails?: boolean;
  bulkActions: {
    changeState: (ids: Array<RefId>, isActivation: boolean) => void;
    updateAttributes: (ids: Array<RefId>, attributes: Attributes) => void;
    deleteEntity: (ids: Array<RefId>) => void;
  };
  onDelete: () => void;
  children: Element<*>;
  extraActions: Array<any>,
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

const deleteHandler = function(props: Props): Function {
  return (allChecked, toggledIds) => {
    const {deleteEntity} = props.bulkActions;

    return (
      <DeleteModal
        count={toggledIds.length}
        stateTitle={'Archive'}
        onConfirm={() => deleteEntity(toggledIds, props.entity, props.onDelete)}
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
      onCancel={() => {}}
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
  const { entity,hideAlertDetails, extraActions } = props;
  const module = `${entity}s`;
  const stateActions = (entity == 'coupon') ? [] : [
    ['Activate', changeStateHandler(props, false), 'successfully activated', 'could not be activated'],
    ['Deactivate', changeStateHandler(props, false), 'successfully deactivated', 'could not be deactivated'],
    [`Schedule ${entity}s`, scheduleHandler(props), 'successfully updated', 'could not be updated'],
  ];
  const extras = _.isEmpty(extraActions) ? [] : extraActions;
  const deleteAction = ['Archive', deleteHandler(props), 'successfully archived', 'could not be archived'];

  const bulkActions = [
    ...extras,
    deleteAction,
    ...stateActions
  ];

  return (
    <div>
      <BulkMessages
        storePath={`${module}.bulk`}
        module={module}
        entity={entity}
        hideAlertDetails={hideAlertDetails}
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
