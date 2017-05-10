/* @flow */
import { flow, map, filter } from 'lodash/fp';

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
export const bulkExportBulkAction = (modal: (checked: boolean, ids: Array<any>) => any, entityName: string): Array<any> => {
  return [
    `Export Selected ${entityName}`,
    modal,
    'successfully exported',
    'could not be exported',
  ];
};
