
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import MultiSelectRow from '../../table/multi-select-row';
import Initials from '../../user-initials/initials';
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
                  className="fc-store-credits__status-dropdown"
                  items={ activeStateTransitions }
                  placeholder={ currentState }
                  value={ rowState }
                  detached={true}
                  onChange={(value) => changeState(rowId, value)} />
      );
    case 'onHold':
      return (
        <Dropdown name="state"
                  className="fc-store-credits__status-dropdown"
                  items={ onHoldStateTransitions }
                  placeholder={ currentState }
                  value={ rowState }
                  detached={true}
                  onChange={(value) => changeState(rowId, value)} />
      );
    default:
      return (<span>{currentState}</span>);
  }
};

const setCellContents = (sc, field) => {
  if (field === 'state') {
    return stateChanger(sc.id, sc.state, sc.changeState);
  }

  if (field === 'issuedBy') {
    const admin = _.get(sc, 'storeAdmin');

    if (_.isEmpty(admin)) return null;

    return (
      <div><Initials {...admin} />{admin.name}</div>
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
  params: PropTypes.object.isRequired,
};

export default StoreCreditRow;
