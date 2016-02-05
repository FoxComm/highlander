
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import MultiSelectRow from '../../table/multi-select-row';
import Initials from '../../users/initials';
import OriginType from '../../common/origin-type';

const setCellContents = (sc, field) => {
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
  const { storeCredit, columns } = props;

  const key = `sc-${storeCredit.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={storeCredit}
      setCellContents={setCellContents} />
  );
};

StoreCreditRow.propTypes = {
  storeCredit: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired
};

export default StoreCreditRow;
