import PropTypes from 'prop-types';

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
    'change',
    'raw',
  ]),
  text: PropTypes.string,
  colSpan: PropTypes.number
});

export default columnType;
