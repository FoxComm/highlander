/* @flow */

export const isDirectField = (field: ?string): boolean => !field || field.indexOf('.') === -1;

export const getNestedPath = (field: ?string): string => field ? field.substring(0, field.lastIndexOf('.')): '';
