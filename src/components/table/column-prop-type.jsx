import { PropTypes } from 'react';

const columnType = PropTypes.shape({
  field: PropTypes.string.isRequired,
  type: PropTypes.oneOf([
    'id',
    'image',
    'state',
    'currency',
    'transaction',
    'moment',
    'date',
    'datetime',
    'time',
    'raw'
  ]),
  text: PropTypes.string,
  colSpan: PropTypes.number
});

export default columnType;
