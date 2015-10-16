'use strict';

import React from 'react';
import TableView from './tableview';
import EditButton from '../common/edit-button';

let EditableTableView = (props) => {
  return (
    <section className='fc-content-box'>
      <header>
        <div className='fc-grid'>
          <div className='fc-col-md-2-3'>{props.title}</div>
          <div className='fc-col-md-1-3 fc-align-right'>
            <EditButton onClick={() => props.editAction()} />
          </div>
        </div>
      </header>
      <TableView columns={props.columns} rows={props.rows} />
    </section>
  );
}

export default EditableTableView;