/* @flow */
import React, { Element } from 'react';

import _ from 'lodash';
import { flow, map, filter, getOr, invoke, reduce, set } from 'lodash/fp';

import { BulkExportModal } from 'components/bulk-actions/modal';

/**
  @propName - a property name used for constructing @props (e.g. referenceNumber, code, etc.)

  @props - an array of elements toggled during the selection, designated by @propName

  @list - an array of objects containing the elements of the table (e.g. list of orders/customers, etc)
*/
export const getIdsByProps = (propName: string, props: Array<any>, list: Array<Object>): Array<number> => {
  return flow(
    filter(entry => props.indexOf(entry[propName]) !== -1),
    map(e => e.id),
  )(list);
};

/**
  @modal - a pass by reference function that returns a confirmation modal

  @entityName - the name of the entity to export
*/
export const bulkExportBulkAction = (
  modal: (checked: boolean, ids: Array<any>) => any,
  entityName: string
): Array<any> => {
  return [
    `Export Selected ${entityName}`,
    modal,
    'successfully exported',
    'could not be exported',
  ];
};

/**
  @entity - an entity you perform the action on (e.g. orders, carts, promotions, etc.)

  @ids - ids for the elements

  @props - the properties you want to extract, can only have a maximum of two

  @state - a result of getState() from redux store
*/
export const getPropsByIds = (
  entity: string,
  ids: Array<number>,
  props: Array<string>,
  state: any
): Object => {
  const sameProps = props.length === 1;
  const prop1 = props[0];
  const prop2 = sameProps ? prop1 : props[1];

  return flow(
    invoke(`${entity}.list.currentSearch`),
    getOr([], 'results.rows'),
    filter(entry => ids.indexOf(entry.id) !== -1),
    reduce((obj, entry) => set(entry[prop1], entry[prop2], obj), {})
  )(state);
};

/**
  @tableColumns - columns of the entity's table (e.g. referenceNumber, state, customer.name, etc.)

  @toggledIds - ids of the toggled elements of the table

  @exportByIds - redux action to start the export

  @title - a title for the modal

  @entity - the name of the entity (e.g. giftCards, promotions, etc.)
*/
export const renderExportModal = (
  tableColumns: Array<Object>,
  toggledIds: Array<number>,
  exportByIds: Function,
  title: string,
  entity: string
): Element<*> => {
  const fields = _.map(tableColumns, c => c.field);
  const identifier = _.map(tableColumns, item => item.text).toString();
  return (
    <BulkExportModal
      count={toggledIds.length}
      onConfirm={(description) => exportByIds(toggledIds, description, fields, entity, identifier)}
      title={title}
    />
  );
};