
// libs
import React from 'react';
import types from '../base/types';

// components
import OrderTarget from '../base/order-target';
import Person from '../base/person';

[
  types.ASSIGNED_TO_ORDER,
  types.UNASSIGNED_FROM_ORDER,
  types.BULK_ASSIGNED_TO_ORDERS,
  types.BULK_UNASSIGNED_FROM_ORDERS,
]

const representatives = {
  [types.ASSIGNED_TO_ORDER]: {
    title: data => {
      if (data.assignees.length == 1) {
        const person = data.assignees[0];

        return (
          <span>
            <strong>assigned</strong> <Person {...person} /> to <OrderTarget order={data.order} />.
          </span>
        );
      } else {
        return (
          <span>
            <strong>assigned</strong> a set of persons to <OrderTarget order={data.order} />.
          </span>
        );
      }
    },
    details: data => {
      if (data.assignees.length > 1) {
        return (
          <div>
            <div className="fc-activity__details-head">Assignees</div>
            <ul>
              {data.assignees.map(person => {
                return (
                  <li>
                    <Person {...person} />
                  </li>
                );
              })}
            </ul>
          </div>
        );
      }
    }
  },
};

export default representatives;
