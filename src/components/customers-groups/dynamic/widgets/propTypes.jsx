import { PropTypes } from 'react';

const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  classname: PropTypes.string.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func,
};

export default propTypes;
