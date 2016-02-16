
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableRow from '../table/row';

const Drawer = props => {
  const drawerClass = classNames('fc-expandable-table__drawer', {
    '_shown': props.isVisible,
    '_hidden': !props.isVisible,
  });

  return (
    <TableRow className={drawerClass}>
      <td colSpan={props.colspan}>
        {props.children}
      </td>
    </TableRow>
  );
};

Drawer.propTypes = {
  isVisible: PropTypes.bool,
  colspan: PropTypes.number,
  children: PropTypes.node,
};

Drawer.defaultProps = {
  isVisible: false,
  colspan: 1,
};

export default Drawer;
