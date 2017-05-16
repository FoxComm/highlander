import PropTypes from 'prop-types';

const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  className: PropTypes.string.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func,
};

export default propTypes;
