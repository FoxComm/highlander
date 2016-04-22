/* @flow */

export const isDirectField = (field: string): boolean => field.indexOf('.') === -1;

export const getNestedPath = (field: string): string => field.substring(0, field.lastIndexOf('.'));
