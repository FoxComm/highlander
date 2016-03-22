//libs
import React, { PropTypes } from 'react';

//data
import operators from '../../paragons/customer-groups/operators';


export default class DynamicGroup extends React.Component {

  static propTypes = {
    group: PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      mainCondition: PropTypes.oneOf([
        operators.and,
        operators.or,
      ]),
      conditions: PropTypes.arrayOf(PropTypes.array),
    }),
  };

  render() {
    const {group} = this.props;

    return (
      <div>
        Dynamic Group `{group.name}` details
      </div>
    );
  }
}
