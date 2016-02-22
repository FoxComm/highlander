
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

// components
import ExpandableRow from '../table/expandable-row';
import Table from '../table/table';

const setCellContents = (warehouse, field, params) => {
  return _.get(warehouse, field);
};

const setDrawerContent = (row, params) => {
  return (
    <div>
      <Table
        className="fc-inventory-item-details__warehouse-details-table"
        columns={params.drawerColumns}
        data={params.drawerData(row.id)} />
    </div>
  );
};

class InventoryWarehouseRow extends React.Component {

  render() {
    const { warehouse, columns, params } = this.props;
    const key = `inventory-list-${warehouse.id}`;

    return (
      <ExpandableRow
        key={key}
        cellKeyPrefix={key}
        columns={columns}
        onClick={_.noop}
        row={warehouse}
        params={params}
        setCellContents={setCellContents}
        setDrawerContent={setDrawerContent} />
    );
  }
};

InventoryWarehouseRow.propTypes = {
  warehouse: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
  fetchDetails: PropTypes.func.isRequired,
};

export default InventoryWarehouseRow;
