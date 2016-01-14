
import React from 'react';
import ActivityTrail from './activity-trail';
import types from './activities/base/types';
import { processActivity } from '../../modules/activity-trail';
import moment from 'moment';

function addContext(activity, i) {
  let userType;

  if (activity.context) {
    userType = activity.context.userType;
  } else {
    userType = i % 2 ? 'system' : 'admin';
    if (activity.data.admin) {
      userType = 'admin';
    }
    activity.context = {userType};
  }

  if (userType == 'admin' && !activity.data.admin) {
    activity.data.admin = {
      name: 'Jon Doe'
    };
  }

  return activity;
}

const customer = {
  id: 1,
  name: 'Thomas Angelo',
  email: 'angelo@salieri.it',
  phoneNumber: '+11111111111',
};

const admin = {
  id: 2,
  name: 'Ennio Salieri',
  email: 'don@salieri.it',
  phoneNumber: '+77777777777',
};

const address = {
  "id": 3,
  "region": {
    "id": 4177,
    "countryId": 234,
    "name": "California"
  },
  "name": "South",
  "address1": "555 E Lake Union St.",
  "city": "Los Angeles",
  "zip": "54321",
  "isDefault": false
};

const order = {
  referenceNumber: 'BR10001',
  orderStatus: 'cart',
  shippingAddress: {
    "id": 3,
    "region": {
      "id": 4177,
      "countryId": 234,
      "name": "Washington"
    },
    "name": "Home",
    "address1": "555 E Lake Union St.",
    "city": "Seattle",
    "zip": "12345",
    "isDefault": false
  }
};

let createdAt = moment().toString();

let activities = [
  // customers
  {
    kind: types.CUSTOMER_UPDATED,
    createdAt,
    data: {
      customerId: 1,
      oldInfo: {
        name: 'William Shockley',
        email: 'William@Shockley.com',
        phoneNumber: '+11111111111'
      },
      newInfo: {
        name: 'Shockley William',
        email: 'William@Shockley.com',
        phoneNumber: '+11111111111'
      }
    }
  },
  {
    kind: types.CUSTOMER_CREATED,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_REGISTERED,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_ACTIVATED,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_BLACKLISTED,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_REMOVED_FROM_BLACKLIST,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_ENABLED,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_DISABLED,
    createdAt,
    data: {
      customer
    }
  },
];

createdAt = moment().subtract(1, 'days').toString();

activities = [...activities,

  // customer addresses

  {
    kind: types.CUSTOMER_ADDRESS_CREATED_BY_ADMIN,
    createdAt,
    data: {
      customer,
      admin,
      address
    },
    context: {
      userType: 'admin'
    }
  },
  {
    kind: types.CUSTOMER_ADDRESS_CREATED,
    createdAt,
    data: {
      customer,
      address
    },
    context: {
      userType: 'customer'
    }
  },
  {
    kind: types.CUSTOMER_ADDRESS_UPDATED,
    createdAt,
    data: {
      customer,
      oldInfo: address,
      newInfo: {
        ...address,
        zip: '23414',
      },
    },
  },
  {
    kind: types.CUSTOMER_ADDRESS_DELETED,
    createdAt,
    data: {
      customer,
      address,
    },
  },
];

createdAt = moment().subtract(2, 'days').toString();

activities = [...activities,

  // order shipping address

  {
    kind: types.ORDER_SHIPPING_ADDRESS_UPDATED,
    createdAt,
    data: {
      order,
      address: {
        "id": 3,
        "region": {
          "id": 4177,
          "countryId": 234,
          "name": "California"
        },
        "name": "South",
        "address1": "555 E Lake Union St.",
        "city": "Los Angeles",
        "zip": "54321",
        "isDefault": false
      }
    }
  },
  {
    kind: types.ORDER_SHIPPING_ADDRESS_ADDED,
    createdAt,
    data: {
      order,
      address
    }
  },
  {
    kind: types.ORDER_SHIPPING_ADDRESS_REMOVED,
    createdAt,
    data: {
      order,
      address
    }
  },

  // order notes
  {
    kind: types.ORDER_NOTE_CREATED,
    createdAt,
    data: {
      orderRefNum: 'BR10001',
      text: 'New note for order.'
    }
  },

];

createdAt = moment().subtract(3, 'days').toString();

activities = [...activities,

  // orders
  {
    kind: types.ORDER_STATE_CHANGED,
    createdAt,
    data: {
      order: {
        referenceNumber: 'BR10004',
        orderStatus: 'fraudHold'
      },
      oldState: 'manualHold',
    }
  },
  {
    kind: types.ORDER_BULK_STATE_CHANGED,
    createdAt,
    data: {
      newState: 'fraudHold',
      orders: [
        'BR10004',
        'BR10003',
        'BR10001',
      ],
      oldState: 'manualHold',
    }
  },
  {
    kind: types.CART_CREATED,
    createdAt,
    data: {
      order,
    }
  }
];

activities = activities.map(processActivity).map(addContext);

export default class AllActivities extends React.Component {

  render() {
    return (
      <div style={{margin: '20px'}}>
        <ActivityTrail activities={activities} hasMore={false} />;
      </div>
    );
  }
}
