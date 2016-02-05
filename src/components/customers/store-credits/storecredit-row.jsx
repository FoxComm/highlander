
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import MultiSelectRow from '../../table/multi-select-row';
import Initials from '../../users/initials';
import OriginType from '../../common/origin-type';
import State from '../../common/state';
import Dropdown from '../../dropdown/dropdown';

const activeStateTransitions = [
  ['onHold', 'On Hold'],
  ['canceled', 'Cancel Store Credit'],
];

const onHoldStateTransitions = [
  ['active', 'Active'],
  ['canceled', 'Cancel Store Credit'],
];

const stateChanger = (rowId, rowState, changeState) => {
  const currentState = <State value={rowState} model="storeCredit"/>;
  switch(rowState) {
    case 'active':
      return (
        <Dropdown name="state"
                  items={ activeStateTransitions }
                  placeholder={ currentState }
                  value={ rowState }
                  onChange={(value) => changeState(rowId, value)} />
      );
    case 'onHold':
      return (
        <Dropdown name="state"
                  items={ onHoldStateTransitions }
                  placeholder={ currentState }
                  value={ rowState }
                  onChange={(value) => changeState(rowId, value)} />
      );
    default:
      return (<span>{rowState}</span>);
  }
};

const setCellContents = (sc, field) => {
  if (field === 'state') {
    console.log(field);
    const state = sc.state;
    const id = sc.id;
    return stateChanger(id, state, sc.changeState);
  }

  if (field === 'issuedBy') {
    const admin = _.get(sc, 'storeAdmin');

    if (_.isEmpty(admin)) return null;

    return (
      <div><Initials name={admin.name} email={admin.email} />{admin.name}</div>
    );
  }

  if (field === 'originType') {
    return (
      <OriginType value={sc}/>
    );
  }

  return _.get(sc, field, null);
};

const StoreCreditRow = props => {
  const { storeCredit, columns, changeState, params } = props;
  const rowData = {
    ...storeCredit,
    changeState,
  };

  const key = `sc-${storeCredit.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      params={params}
      row={rowData}
      setCellContents={setCellContents} />
  );
};

StoreCreditRow.propTypes = {
  storeCredit: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  changeState: PropTypes.func.isRequired,
};

export default StoreCreditRow;
