
// libs
import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableRow from '../table/row';

const Drawer = props => {
  const drawerClass = classNames('fc-expandable-table__drawer', {
    '_shown': props.params.isOpen,
    '_hidden': !props.params.isOpen,
  });

  return (
    <TableRow className={drawerClass}>
      <td className="fc-expandable-table__drawer-cell" colSpan={props.params.colSpan}>
        {props.children}
      </td>
    </TableRow>
  );
};

Drawer.propTypes = {
  params: PropTypes.shape({
    isOpen: PropTypes.bool,
    colSpan: PropTypes.number,
  }),
  children: PropTypes.node,
};

Drawer.defaultProps = {
  params: {
    isOpen: false,
    colSpan: 1,
  },
};

export default Drawer;
