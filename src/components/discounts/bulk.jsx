/* @flow */

import _ from 'lodash';
import React, { Element } from 'react';
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
    updateAttributes: (ids: Array<RefId>, form: FormAttributes, shadow: ShadowAttributes) => void;
  };
  children: Element;
};

const mapDispatchToProps = (dispatch: Function, props) => {
  const module = `${props.entity}s`;

  const {actions} = getStore('bulk', module);
  return {
    bulkActions: bindActionCreators(actions, dispatch),
  };
};


const BulkWrapper = (props: Props) => {
  const { entity } = props;
  const module = `${entity}s`;

  const changeStateHandler = function(isActivation) {
    const stateTitle = isActivation ? 'Active' : 'Inactive';

    return (allChecked, toggledIds) => {
      const {changeState} = props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => changeState(toggledIds, isActivation)} />
      );
    };
  };

  const scheduleHandler = (allChecked, toggledIds) => {
    const {updateAttributes} = props.bulkActions;

    const handleConfirm = (form, shadow) => {
      updateAttributes(toggledIds, form, shadow);
    };

    return (
      <SchedulerModal
        entity={entity}
        count={toggledIds.length}
        onConfirm={handleConfirm}
      />
    );
  };

  const bulkActions = [
    ['Activate', changeStateHandler(true), 'successfully activated', 'could not be activated'],
    ['Deactivate', changeStateHandler(false), 'successfully deactivated', 'could not be deactivated'],
    [`Schedule ${entity}s`, scheduleHandler, 'successfully updated', 'could not be updated'],
  ];

  const entityCap = _.capitalize(entity);

  const renderDetail = (messages, id) => {
    const idParam = `${entity}Id`;
    const body = _.isEmpty(messages) ? null : [': ', messages];

    return (
      <span key={id}>
        {entityCap} <Link to={`${entity}-details`} params={{[idParam]: id}}>{id}</Link>{body}
      </span>
    );
  };

  return (
    <div>
      <BulkMessages
        storePath={`${module}.bulk`}
        module={module}
        entity={entity}
        renderDetail={renderDetail} />
      <BulkActions
        module={module}
        entity={entity}
        actions={bulkActions}>
        {props.children}
      </BulkActions>
    </div>
  );
};

export default connect(null, mapDispatchToProps)(BulkWrapper);

