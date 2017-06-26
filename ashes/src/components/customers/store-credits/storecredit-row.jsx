/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import Initials from 'components/user-initials/initials';
import OriginType from 'components/common/origin-type';
import State from 'components/common/state';
import Dropdown from 'components/dropdown/dropdown';

const activeStateTransitions = [
  ['onHold', 'On Hold'],
  ['canceled', 'Cancel Store Credit'],
];

const onHoldStateTransitions = [
  ['active', 'Active'],
  ['canceled', 'Cancel Store Credit'],
];

type Props = {
  storeCredit: Object,
  columns: Columns,
  changeState: (rowId: number, state: string) => Array<any>,
  params: Object,
};

const StoreCreditRow = (props: Props) => {
  const { storeCredit, columns, changeState, params } = props;
  const rowData = {
    ...storeCredit,
    changeState,
  };

  const stateChanger = (
    rowId: number, rowState: string, changeState: (rowId: number, state: string) => Array<any>
  ) => {
    const currentState = <State value={rowState} model="storeCredit" />;

    switch(rowState) {
      case 'active':
        return (
          <Dropdown
            name="state"
            className="fc-store-credits__status-dropdown"
            items={ activeStateTransitions }
            placeholder={ currentState }
            value={ rowState }
            detached={true}
            onChange={(value) => changeState(rowId, value)}
          />
        );
      case 'onHold':
        return (
          <Dropdown
            name="state"
            className="fc-store-credits__status-dropdown"
            items={ onHoldStateTransitions }
            placeholder={ currentState }
            value={ rowState }
            detached={true}
            onChange={(value) => changeState(rowId, value)}
          />
        );
      default:
        return (
          <span>{currentState}</span>
        );
    }
  };

  const setCellContents = (sc: Object, field: string) => {
    const admin = _.get(sc, 'storeAdmin');

    switch(field) {
      case 'state':
        return stateChanger(sc.id, sc.state, sc.changeState);
      case 'issuedBy':
        if (_.isEmpty(admin)) return null;

        return (
          <div><Initials {...admin} />{admin.name}</div>
        );
      case 'originType':
        return (
          <OriginType value={sc} />
        );
      default:
        return _.get(sc, field, null);
    }
  };

  return (
    <MultiSelectRow
      columns={columns}
      params={params}
      row={rowData}
      setCellContents={setCellContents}
    />
  );
};

export default StoreCreditRow;
