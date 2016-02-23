
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableRow from '../table/row';

const Drawer = props => {
  const drawerClass = classNames('fc-expandable-table__drawer', {
    '_shown': props.params.isOpen,
    '_hidden': !props.params.isOpen,
  });

  console.log('drawer');
  console.log(props);

  return (
    <TableRow className={drawerClass}>
      <td className="fc-expandable-table__drawer-cell" colSpan={props.params.colspan}>
        {props.children}
      </td>
    </TableRow>
  );
};

Drawer.propTypes = {
  isOpen: PropTypes.bool,
  colspan: PropTypes.number,
  children: PropTypes.node,
};

Drawer.defaultProps = {
  params: {
    isOpen: false,
    colspan: 1,
  },
};

export default Drawer;
